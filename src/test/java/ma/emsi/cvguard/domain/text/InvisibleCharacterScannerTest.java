package ma.emsi.cvguard.domain.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification du releve des caracteres invisibles et des marques directionnelles
 * (chapitre 1, regle R4).
 */
class InvisibleCharacterScannerTest {

    @Test
    @DisplayName("Un texte ordinaire ne produit aucune occurrence")
    void ordinaryTextHasNoMatches() {
        assertThat(InvisibleCharacterScanner.scan("Ingenieur en informatique")).isEmpty();
        assertThat(InvisibleCharacterScanner.density("Ingenieur en informatique")).isZero();
    }

    @Test
    @DisplayName("L'espace de largeur nulle est detectee, a la bonne position")
    void detectsZeroWidthSpace() {
        List<InvisibleCharacterMatch> matches = InvisibleCharacterScanner.scan("mot​cache");

        assertThat(matches).singleElement().satisfies(match -> {
            assertThat(match.index()).isEqualTo(3);
            assertThat(match.codePoint()).isEqualTo(0x200B);
            assertThat(match.hex()).isEqualTo("U+200B");
        });
    }

    @Test
    @DisplayName("Les marques directionnelles citees par le cahier des charges sont couvertes")
    void detectsCahierExamples() {
        // Chapitre 1 : "U+200B, U+200E, U+FEFF"
        assertThat(InvisibleCharacterScanner.isInvisible(0x200B)).isTrue();
        assertThat(InvisibleCharacterScanner.isInvisible(0x200E)).isTrue();
        assertThat(InvisibleCharacterScanner.isInvisible(0xFEFF)).isTrue();
    }

    @Test
    @DisplayName("Un caractere accentue ordinaire n'est pas confondu avec une marque invisible")
    void accentedLetterIsNotAMatch() {
        assertThat(InvisibleCharacterScanner.isInvisible('é')).isFalse();
    }

    @Test
    @DisplayName("La densite rapporte le nombre d'occurrences a la longueur du texte")
    void densityIsComputedOverLength() {
        // 2 occurrences sur 10 caracteres
        String text = "​abcdefgh​";

        assertThat(InvisibleCharacterScanner.density(text)).isCloseTo(0.2d, within(0.0001d));
    }

    @Test
    @DisplayName("Plusieurs occurrences sont toutes relevees, dans l'ordre d'apparition")
    void reportsAllOccurrencesInOrder() {
        String text = "a​b‌c﻿d";

        List<InvisibleCharacterMatch> matches = InvisibleCharacterScanner.scan(text);

        assertThat(matches).extracting(InvisibleCharacterMatch::codePoint)
                .containsExactly(0x200B, 0x200C, 0xFEFF);
    }

    @Test
    @DisplayName("Une entree vide ou nulle ne produit aucune occurrence")
    void handlesEmptyAndNullInput() {
        assertThat(InvisibleCharacterScanner.scan("")).isEmpty();
        assertThat(InvisibleCharacterScanner.scan(null)).isEmpty();
        assertThat(InvisibleCharacterScanner.density(null)).isZero();
    }
}
