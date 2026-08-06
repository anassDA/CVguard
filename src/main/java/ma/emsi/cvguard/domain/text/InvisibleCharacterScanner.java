package ma.emsi.cvguard.domain.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Releve des caracteres invisibles et des marques directionnelles (etape [2], regle
 * R4) : "caracteres invisibles ou de largeur nulle (U+200B, U+200E, U+FEFF) inseres
 * entre les mots" (chapitre 1).
 *
 * <p>Cette classe se limite au releve des occurrences ; leur qualification en
 * detection, avec seuil de densite et ponderation, releve de la regle R4 elle-meme.
 * Le partage de cette logique de scan evite que la regle et une eventuelle politique
 * d'assainissement ne divergent sur la definition d'un caractere invisible.</p>
 */
public final class InvisibleCharacterScanner {

    /**
     * Points de code retenus, avec leur designation. La liste couvre les deux familles
     * decrites par le cahier des charges : les caracteres de largeur nulle (espaces et
     * jointures invisibles, marqueur d'ordre des octets) et les marques directionnelles
     * (marques simples, embarquements, isolats).
     */
    private static final Map<Integer, String> INVISIBLE_CODE_POINTS = Map.ofEntries(
            Map.entry(0x200B, "ZERO WIDTH SPACE"),
            Map.entry(0x200C, "ZERO WIDTH NON-JOINER"),
            Map.entry(0x200D, "ZERO WIDTH JOINER"),
            Map.entry(0x2060, "WORD JOINER"),
            Map.entry(0xFEFF, "ZERO WIDTH NO-BREAK SPACE"),
            Map.entry(0x200E, "LEFT-TO-RIGHT MARK"),
            Map.entry(0x200F, "RIGHT-TO-LEFT MARK"),
            Map.entry(0x202A, "LEFT-TO-RIGHT EMBEDDING"),
            Map.entry(0x202B, "RIGHT-TO-LEFT EMBEDDING"),
            Map.entry(0x202C, "POP DIRECTIONAL FORMATTING"),
            Map.entry(0x202D, "LEFT-TO-RIGHT OVERRIDE"),
            Map.entry(0x202E, "RIGHT-TO-LEFT OVERRIDE"),
            Map.entry(0x2066, "LEFT-TO-RIGHT ISOLATE"),
            Map.entry(0x2067, "RIGHT-TO-LEFT ISOLATE"),
            Map.entry(0x2068, "FIRST STRONG ISOLATE"),
            Map.entry(0x2069, "POP DIRECTIONAL ISOLATE"));

    private InvisibleCharacterScanner() {
    }

    /** Indique si le point de code donne appartient a l'ensemble retenu. */
    public static boolean isInvisible(int codePoint) {
        return INVISIBLE_CODE_POINTS.containsKey(codePoint);
    }

    /** Releve chaque occurrence, dans l'ordre d'apparition. */
    public static List<InvisibleCharacterMatch> scan(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<InvisibleCharacterMatch> matches = new ArrayList<>();
        int index = 0;
        int length = text.length();
        while (index < length) {
            int codePoint = text.codePointAt(index);
            String name = INVISIBLE_CODE_POINTS.get(codePoint);
            if (name != null) {
                matches.add(new InvisibleCharacterMatch(index, codePoint, name));
            }
            index += Character.charCount(codePoint);
        }
        return matches;
    }

    /**
     * Densite de caracteres invisibles, en fraction de la longueur du texte.
     *
     * @return une valeur dans [0, 1], nulle pour un texte vide
     */
    public static double density(String text) {
        if (text == null || text.isEmpty()) {
            return 0d;
        }
        long invisibleCount = scan(text).size();
        return (double) invisibleCount / text.length();
    }
}
