package ma.emsi.cvguard.domain.rule.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.RgbColor;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification de la regle R6 (EF-12, chapitre 8) : l'ecart de similarite entre le
 * texte natif et le texte OCR du rendu. Le texte OCR est depose par l'orchestrateur
 * dans l'espace de faits partages ; en son absence, la regle se retire.
 */
class OcrDiscrepancyRuleTest {

    private final OcrDiscrepancyRule rule = new OcrDiscrepancyRule();

    @Test
    @DisplayName("La regle se retire lorsque le controle OCR n'a pas ete exerce")
    void retiresWithoutOcrText() {
        RuleContext context = context("texte natif", null);
        assertThat(rule.supports(context)).isFalse();
        assertThat(rule.apply(context)).isEmpty();
    }

    @Test
    @DisplayName("Un ecart significatif entre texte natif et texte OCR est signale")
    void detectsDivergenceBetweenNativeAndRenderedText() {
        // Le texte natif porte en tete une phrase blanche sur blanc, absente du rendu OCR.
        RuleContext context = context(
                "phrase invisible en blanc sur blanc a supprimer. Texte visible uniquement.",
                "Texte visible uniquement.");

        assertThat(rule.supports(context)).isTrue();
        var detections = rule.apply(context);

        assertThat(detections).singleElement().satisfies(detection ->
                assertThat(((Number) detection.details().get("gap")).doubleValue()).isGreaterThan(0.15d));
    }

    @Test
    @DisplayName("Un ecart de similarite faible ne declenche pas la regle")
    void smallOcrVariationIsTolerated() {
        // Differences d'OCR ordinaires (ponctuation, espaces) : sous le seuil de 15 %.
        RuleContext context = context("Ingenieur en informatique, specialise en securite applicative.",
                "Ingenieur en informatique specialise en securite applicative");
        assertThat(rule.apply(context)).isEmpty();
    }

    @Test
    @DisplayName("La similarite mesuree est deposee pour le rapport d'inspection")
    void similarityIsExposedAsFact() {
        RuleContext context = context("Texte natif du document.", "Texte natif du document.");
        rule.apply(context);
        assertThat(context.fact(RuleContext.FACT_OCR_SIMILARITY, Double.class))
                .hasValueSatisfying(similarity -> assertThat(similarity).isGreaterThan(0.9d));
    }

    private static RuleContext context(String nativeText, String ocrText) {
        TextFragment fragment = new TextFragment(
                0, 1, nativeText, "Helvetica", 11f,
                RgbColor.BLACK, RgbColor.WHITE, 1f, false, null, FragmentOrigin.BODY);
        ExtractedDocument document = new ExtractedDocument(
                "doc", DocumentFormat.PDF, "cv.pdf", List.of(), List.of(fragment), Map.of());
        RuleContext context = new RuleContext(document, DetectionThresholds.defaults());
        if (ocrText != null) {
            context.putFact(RuleContext.FACT_OCR_TEXT, ocrText);
        }
        return context;
    }
}
