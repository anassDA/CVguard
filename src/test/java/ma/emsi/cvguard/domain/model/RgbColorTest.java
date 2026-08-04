package ma.emsi.cvguard.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification du calcul de contraste WCAG, socle de la regle R2.
 *
 * <p>Les valeurs de reference sont celles de la specification WCAG 2.1 : le contraste
 * du noir sur blanc vaut exactement 21:1, celui de deux couleurs identiques 1:1.</p>
 */
class RgbColorTest {

    @Test
    @DisplayName("Le contraste du noir sur blanc vaut 21:1")
    void blackOnWhiteReachesMaximumContrast() {
        assertThat(RgbColor.BLACK.contrastRatio(RgbColor.WHITE)).isCloseTo(21d, within(0.01d));
    }

    @Test
    @DisplayName("Le blanc sur blanc vaut 1:1 — le vecteur d'attaque le plus courant")
    void whiteOnWhiteIsTheMinimumContrast() {
        assertThat(RgbColor.WHITE.contrastRatio(RgbColor.WHITE)).isCloseTo(1d, within(0.0001d));
    }

    @Test
    @DisplayName("Le ratio est symetrique : l'ordre des couleurs est indifferent")
    void contrastRatioIsSymmetric() {
        RgbColor grey = RgbColor.ofBytes(240, 240, 240);
        assertThat(grey.contrastRatio(RgbColor.WHITE))
                .isEqualTo(RgbColor.WHITE.contrastRatio(grey));
    }

    @Test
    @DisplayName("Un gris tres clair sur blanc reste sous le seuil de 1,5:1")
    void veryLightGreyOnWhiteFallsBelowThreshold() {
        // Cas de l'injection subtile decrite au chapitre 10 : lisible par le parseur,
        // pratiquement invisible a l'ecran.
        RgbColor almostWhite = RgbColor.ofBytes(250, 250, 250);
        assertThat(almostWhite.contrastRatio(RgbColor.WHITE)).isLessThan(1.5d);
    }

    @Test
    @DisplayName("Un gris moyen sur blanc depasse le seuil : ce n'est pas une dissimulation")
    void mediumGreyOnWhiteStaysAboveThreshold() {
        // Filigrane ou mention legale : faible contraste mais intention legitime.
        RgbColor mediumGrey = RgbColor.ofBytes(150, 150, 150);
        assertThat(mediumGrey.contrastRatio(RgbColor.WHITE)).isGreaterThan(1.5d);
    }

    @Test
    @DisplayName("Les composantes hors bornes sont ramenees dans [0, 1]")
    void componentsAreClamped() {
        RgbColor clamped = new RgbColor(3f, -2f, Float.NaN);
        assertThat(clamped.red()).isEqualTo(1f);
        assertThat(clamped.green()).isEqualTo(0f);
        assertThat(clamped.blue()).isEqualTo(0f);
    }

    @Test
    @DisplayName("La representation hexadecimale est celle attendue dans le rapport")
    void hexRepresentation() {
        assertThat(RgbColor.ofBytes(255, 255, 255).toHex()).isEqualTo("#FFFFFF");
        assertThat(RgbColor.ofPacked(0x1A2B3C).toHex()).isEqualTo("#1A2B3C");
    }
}
