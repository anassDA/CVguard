package ma.emsi.cvguard.domain.text;

import java.text.Normalizer;

/**
 * Normalisation Unicode (etape [2] de la chaine de traitement).
 *
 * <p>La forme de compatibilite NFKC ramene les variantes de compatibilite d'un
 * caractere vers leur forme canonique : demi-chasse et pleine chasse, ligatures,
 * exposants, variantes stylistiques. Sans cette etape, un attaquant pourrait rediger
 * son instruction avec des caracteres de compatibilite — texte en pleine chasse
 * notamment — pour echapper a une detection lexicale fondee sur une expression
 * reguliere simple (regle R7, chapitre 8).</p>
 *
 * <p>NFKC ne decompose ni ne supprime les caracteres de controle de largeur nulle
 * (U+200B...) ni les marques directionnelles (U+200E, U+202A...) : ces points de code
 * n'ont pas de correspondance de compatibilite dans la base Unicode. La normalisation
 * peut donc etre appliquee sans effacer les indices que la regle R4 doit detecter.</p>
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * Normalise un texte en forme de compatibilite NFKC.
     *
     * @return le texte normalise, ou {@code null} si l'entree est {@code null}
     */
    public static String nfkc(String text) {
        if (text == null) {
            return null;
        }
        return Normalizer.normalize(text, Normalizer.Form.NFKC);
    }
}
