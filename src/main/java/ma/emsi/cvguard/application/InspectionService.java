package ma.emsi.cvguard.application;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ma.emsi.cvguard.config.CvGuardProperties;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.port.OcrEngine;
import ma.emsi.cvguard.domain.port.OcrException;
import ma.emsi.cvguard.domain.port.PageRenderer;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleContext;
import ma.emsi.cvguard.domain.rule.RuleEngine;
import ma.emsi.cvguard.domain.rule.RuleEngineResult;
import ma.emsi.cvguard.domain.score.RiskAssessment;
import ma.emsi.cvguard.domain.score.ScoreAggregator;
import ma.emsi.cvguard.infrastructure.extraction.UnifiedDocumentExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrateur d'inspection (etape [3] a [7] de la chaine de traitement).
 *
 * <pre>
 * [1] EXTRACTION    UnifiedDocumentExtractor → fragments + attributs de rendu
 * [2] NORMALISATION assuree par les extracteurs (NFKC, releve des invisibles)
 * [3] MOTEUR        regles structurelles R1 a R5
 *      └─ signal leve ? → [4] CONTROLE OCR (rendu → Tesseract → ecart de similarite)
 * [5] ANALYSE       regles R6 et R7
 * [6] AGREGATION    ScoreAggregator → SAFE | SUSPECT | MALICIOUS
 * [7] ASSAINISSEMENT purge des segments + encapsulation (Sanitizer)
 * </pre>
 *
 * <p>Le controle OCR n'est exerce que lorsqu'un premier signal a ete leve (ENF-02) :
 * c'est la regle la plus couteuse, et un document sans aucun signal n'a pas besoin
 * d'etre rendu. L'echec du controle — moteur indisponible, rendu impossible — est
 * absorbe : la regle R6 se retire et l'inspection se poursuit (ENF-06).</p>
 */
@Service
public class InspectionService {

    private static final Logger log = LoggerFactory.getLogger(InspectionService.class);

    private static final Set<RuleCode> STRUCTURAL_RULES =
            EnumSet.of(RuleCode.R1, RuleCode.R2, RuleCode.R3, RuleCode.R4, RuleCode.R5);

    private static final Set<RuleCode> SEMANTIC_RULES = EnumSet.of(RuleCode.R6, RuleCode.R7);

    private final UnifiedDocumentExtractor extractor;
    private final RuleEngine engine;
    private final ScoreAggregator aggregator;
    private final Sanitizer sanitizer;
    private final DetectionThresholds thresholds;
    private final OcrEngine ocrEngine;
    private final PageRenderer pageRenderer;
    private final CvGuardProperties properties;

    public InspectionService(
            UnifiedDocumentExtractor extractor,
            RuleEngine engine,
            ScoreAggregator aggregator,
            Sanitizer sanitizer,
            DetectionThresholds thresholds,
            OcrEngine ocrEngine,
            PageRenderer pageRenderer,
            CvGuardProperties properties) {
        this.extractor = extractor;
        this.engine = engine;
        this.aggregator = aggregator;
        this.sanitizer = sanitizer;
        this.thresholds = thresholds;
        this.ocrEngine = ocrEngine;
        this.pageRenderer = pageRenderer;
        this.properties = properties;
    }

    /**
     * Analyse complete d'un document : extraction, moteur de regles en deux passes,
     * controle OCR conditionnel, agregation du score, assainissement.
     */
    public InspectionOutcome analyze(byte[] content, String filename) {
        String documentId = CorrelationIds.newId();
        long startedAt = System.nanoTime();

        ExtractedDocument document = extractor.extract(content, documentId, filename);
        RuleContext context = new RuleContext(document, thresholds);

        RuleEngineResult structural = engine.run(context, STRUCTURAL_RULES);
        InspectionReport.OcrControlView ocrControl = runOcrControl(context, content, structural.hasDetections());
        RuleEngineResult semantic = engine.run(context, SEMANTIC_RULES);
        RuleEngineResult combined = structural.merge(semantic);

        RiskAssessment assessment = aggregator.aggregate(combined.detections());
        SanitizedContent sanitized = sanitizer.sanitize(document, combined.detections());

        InspectionReport report = assemble(document, context, assessment, combined, ocrControl, sanitized, startedAt);
        log.info("Inspection {} terminee : verdict {} (score {}), {} detection(s), {} purge(s)",
                documentId, assessment.verdict(), assessment.score(),
                combined.detections().size(), sanitized.removedSegments());
        return new InspectionOutcome(document, report, sanitized);
    }

    /**
     * Etape [4] : controle OCR, execute uniquement si un premier signal a ete leve
     * (ENF-02), si le format est rendable et si le moteur est operationnel.
     */
    private InspectionReport.OcrControlView runOcrControl(
            RuleContext context, byte[] content, boolean signalRaised) {
        CvGuardProperties.Ocr ocrConfig = properties.getOcr();
        if (!signalRaised || !ocrConfig.isEnabled()
                || !pageRenderer.canRender(content) || !ocrEngine.isAvailable()) {
            return null;
        }
        try {
            List<BufferedImage> images = pageRenderer.render(content, ocrConfig.getRenderDpi());
            int limit = Math.min(images.size(), Math.max(1, ocrConfig.getMaxPages()));
            StringBuilder recognized = new StringBuilder();
            for (int i = 0; i < limit; i++) {
                recognized.append(ocrEngine.recognize(images.get(i))).append('\n');
            }
            String ocrText = recognized.toString().strip();
            context.putFact(RuleContext.FACT_OCR_TEXT, ocrText);
            return new InspectionReport.OcrControlView(
                    true, null, context.document().nativeText().length(), ocrText.length());
        } catch (RuntimeException failure) {
            log.warn("Controle OCR impossible, la regle R6 se retire : {}", failure.toString());
            return null;
        }
    }

    private InspectionReport assemble(
            ExtractedDocument document,
            RuleContext context,
            RiskAssessment assessment,
            RuleEngineResult result,
            InspectionReport.OcrControlView ocrControl,
            SanitizedContent sanitized,
            long startedAt) {
        int maxExcerpt = properties.getSanitization().getMaxExcerptLength();

        InspectionReport.OcrControlView ocrView = ocrControl == null
                ? null
                : ocrControl.withSimilarity(
                        context.fact(RuleContext.FACT_OCR_SIMILARITY, Double.class).orElse(null));

        return new InspectionReport(
                document.documentId(),
                document.filename(),
                document.format().name(),
                document.pageCount(),
                document.fragments().size(),
                assessment.verdict(),
                assessment.score(),
                result.detections().stream()
                        .map(detection -> InspectionReport.DetectionView.of(detection, maxExcerpt))
                        .toList(),
                toNameMap(assessment.contributions()),
                toNameMap(assessment.occurrences()),
                ocrView,
                result.failures().stream()
                        .map(failure -> failure.rule().name() + " : " + failure.reason())
                        .toList(),
                sanitized.keptLength(),
                sanitized.removedSegments(),
                (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private static Map<String, Integer> toNameMap(Map<RuleCode, Integer> values) {
        return values.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
    }
}
