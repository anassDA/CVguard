package ma.emsi.cvguard.application;

import ma.emsi.cvguard.config.CvGuardProperties;
import ma.emsi.cvguard.domain.port.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Demonstrateur comparatif (EF-13, O6) : le meme document est soumis a deux pipelines,
 * et la reponse du modele de langage est restituee cote a cote.
 *
 * <ul>
 *   <li><b>Pipeline naif</b> — le texte natif du document est transmis tel quel au
 *       modele, accompagne de la consigne d'evaluation. C'est le comportement expose
 *       d'un ATS sans defense : les instructions dissimulees dans le document sont
 *       executees par le modele.</li>
 *   <li><b>Pipeline protege</b> — le document passe par l'inspection complete
 *       (extraction, regles, score, verdict) puis par l'assainissement : les segments
 *       incrimines sont purges et le contenu est encapsule en donnee. Seule cette
 *       version franchit la frontiere vers le modele (ENF-03).</li>
 * </ul>
 *
 * <p>Lorsque aucun modele n'est configure, un client simule reproduit le phenomene —
 * execution de l'instruction par le pipeline naif, analyse conforme par le pipeline
 * protege — et la reponse est etiquetee {@code simulated}.</p>
 */
@Service
public class DemoService {

    private static final Logger log = LoggerFactory.getLogger(DemoService.class);

    private final InspectionService inspectionService;
    private final LlmClient llmClient;
    private final CvGuardProperties properties;

    public DemoService(
            InspectionService inspectionService,
            LlmClient llmClient,
            CvGuardProperties properties) {
        this.inspectionService = inspectionService;
        this.llmClient = llmClient;
        this.properties = properties;
    }

    public DemoReport compare(byte[] content, String filename) {
        String documentId = CorrelationIds.newId();
        log.info("Demonstration comparative sur le document {} : pipeline naif puis pipeline protege", documentId);

        InspectionOutcome outcome = inspectionService.analyze(content, filename);
        String systemInstruction = properties.getLlm().getSystemInstruction();

        String naiveContent = outcome.document().nativeText();
        DemoReport.PipelineView naive = run("NAIVE", naiveContent, systemInstruction);

        String protectedContent = outcome.sanitized().content();
        DemoReport.PipelineView protectedView = run("PROTECTED", protectedContent, systemInstruction);

        return new DemoReport(
                outcome.report().documentId(),
                "simulated".equalsIgnoreCase(properties.getLlm().getMode()),
                outcome.document().filename(),
                outcome.document().format().name(),
                outcome.verdict(),
                outcome.report().riskScore(),
                naive,
                protectedView);
    }

    private DemoReport.PipelineView run(String kind, String content, String systemInstruction) {
        try {
            return new DemoReport.PipelineView(kind, content.length(), llmClient.evaluate(systemInstruction, content), null);
        } catch (RuntimeException failure) {
            log.warn("Pipeline {} en echec : {}", kind, failure.toString());
            return new DemoReport.PipelineView(kind, content.length(), null, failure.getMessage());
        }
    }
}
