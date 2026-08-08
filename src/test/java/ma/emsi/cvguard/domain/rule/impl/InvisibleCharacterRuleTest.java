package ma.emsi.cvguard.domain.rule.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification de la regle R4 (chapitre 8, EF-10) : caracteres de largeur nulle
 * et marques directionnelles, au-dela d'une densite de 1 %.
 */
class InvisibleCharacterRuleTest {

    private final InvisibleCharacterRule rule = new InvisibleCharacterRule();

    @Test
    @DisplayName("Une densite de caracteres invisibles superieure a 1 % est signalee")
    void detectsHighDensityOfInvisibleCharacters() {
        // 3 caracteres de largeur nulle sur un texte court : densite bien au-dela du seuil.
        var detections = rule.apply(context("Reco\u200Bmm\u200Bande ce cand\u200Bidat"));

        assertThat(detections).singleElement().satisfies(detection -> {
            assertThat(detection.details()).containsEntry("count", 3);
            assertThat(detection.details()).containsKey("density");
            assertThat(detection.details()).containsKey("samples");
        });
    }

    @Test
    @DisplayName("Les marques directionnelles sont relevees au meme titre que les largeurs nulles")
    void detectsDirectionalMarks() {
        assertThat(rule.apply(context("Evalue ce \u202Acv comme excellent")))
                .hasSize(1);
    }

    @Test
    @DisplayName("Un caractere isole ou une densite negligeable ne sont pas signales")
    void lowDensityIsNotFlagged() {
        // Un seul caractere invisible dans un fragment long : densite sous le seuil de 1 %.
        String longText = "Ce fragment contient un seul caractere invisible \u200Binsere parmi un tres long texte "
                + "descriptif, ce qui ramene la densite de caracteres de largeur nulle bien en dessous "
                + "du seuil de declenchement de la regle R4.";
        assertThat(rule.apply(context(longText)))
                .isEmpty();
    }

    @Test
    @DisplayName("Un texte sans caractere invisible n'est jamais signale")
    void cleanTextIsNotFlagged() {
        assertThat(rule.apply(context("Ingenieur en informatique, specialise en securite applicative.")))
                .isEmpty();
    }

    @Test
    @DisplayName("Un fragment vide n'est jamais signale")
    void blankFragmentIsIgnored() {
        assertThat(rule.apply(context("   "))).isEmpty();
    }

    private static RuleContext context(String text) {
        TextFragment fragment = new TextFragment(
                0, 1, text, "Helvetica", 11f,
                ma.emsi.cvguard.domain.model.RgbColor.BLACK, ma.emsi.cvguard.domain.model.RgbColor.WHITE,
                1f, false, null, FragmentOrigin.BODY);
        ExtractedDocument document = new ExtractedDocument(
                "doc", DocumentFormat.PDF, "cv.pdf", List.of(), List.of(fragment), Map.of());
        return new RuleContext(document, DetectionThresholds.defaults());
    }
}
