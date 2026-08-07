package ma.emsi.cvguard.domain.rule.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.model.BoundingBox;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.PageGeometry;
import ma.emsi.cvguard.domain.model.RgbColor;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification de la regle R3 (chapitre 8, EF-05), volet hors-page.
 */
class OutOfPageAreaRuleTest {

    private static final PageGeometry A4_PAGE =
            new PageGeometry(1, new BoundingBox(0f, 0f, 595f, 842f), new BoundingBox(0f, 0f, 595f, 842f), 0);

    private final OutOfPageAreaRule rule = new OutOfPageAreaRule();

    @Test
    @DisplayName("Un fragment sous la page (ordonnee negative) est signale")
    void detectsFragmentBelowPage() {
        TextFragment offPage = fragment(72f, -50f, 200f, 11f);

        assertThat(rule.apply(context(List.of(offPage), List.of(A4_PAGE)))).hasSize(1);
    }

    @Test
    @DisplayName("Un fragment au-dela du bord droit de la page est signale")
    void detectsFragmentBeyondRightEdge() {
        TextFragment offPage = fragment(550f, 700f, 200f, 11f);

        assertThat(rule.apply(context(List.of(offPage), List.of(A4_PAGE)))).hasSize(1);
    }

    @Test
    @DisplayName("Un fragment entierement dans la page n'est pas signale")
    void fragmentWithinPageIsNotFlagged() {
        TextFragment onPage = fragment(72f, 700f, 200f, 11f);

        assertThat(rule.apply(context(List.of(onPage), List.of(A4_PAGE)))).isEmpty();
    }

    @Test
    @DisplayName("Un leger debordement dans la tolerance n'est pas signale")
    void slightOverflowWithinToleranceIsNotFlagged() {
        // Tolerance par defaut : 2 pt (chapitre 8).
        TextFragment barelyOver = fragment(72f, -1f, 200f, 11f);

        assertThat(rule.apply(context(List.of(barelyOver), List.of(A4_PAGE)))).isEmpty();
    }

    @Test
    @DisplayName("Un fragment DOCX, sans position connue, n'est jamais signale")
    void fragmentsWithoutKnownPositionAreIgnored() {
        TextFragment noPosition = new TextFragment(
                0, 0, "texte DOCX", "Calibri", 11f, RgbColor.BLACK, RgbColor.WHITE, 1f, false, null,
                FragmentOrigin.BODY);

        assertThat(rule.apply(context(List.of(noPosition), List.of(A4_PAGE)))).isEmpty();
    }

    @Test
    @DisplayName("La regle ne s'applique pas a un document sans geometrie de page")
    void ruleDoesNotSupportDocumentsWithoutPages() {
        RuleContext context = context(List.of(fragment(72f, 700f, 200f, 11f)), List.of());

        assertThat(rule.supports(context)).isFalse();
    }

    @Test
    @DisplayName("La regle s'applique a un document dont la geometrie de page est connue")
    void ruleSupportsDocumentsWithPages() {
        RuleContext context = context(List.of(fragment(72f, 700f, 200f, 11f)), List.of(A4_PAGE));

        assertThat(rule.supports(context)).isTrue();
    }

    @Test
    @DisplayName("Le code de regle expose est R3")
    void exposesRuleCode() {
        assertThat(rule.code().name()).isEqualTo("R3");
    }

    private static RuleContext context(List<TextFragment> fragments, List<PageGeometry> pages) {
        ExtractedDocument document = new ExtractedDocument(
                "doc", DocumentFormat.PDF, "cv.pdf", pages, fragments, Map.of());
        return new RuleContext(document, DetectionThresholds.defaults());
    }

    private static TextFragment fragment(float x, float y, float width, float fontSizePt) {
        return new TextFragment(
                0, 1, "texte", "Helvetica", fontSizePt, RgbColor.BLACK, RgbColor.WHITE, 1f, false,
                new BoundingBox(x, y, width, fontSizePt), FragmentOrigin.BODY);
    }
}
