package ma.emsi.cvguard.domain.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Analyse lexicale des segments rediges sous forme d'instruction adressee a un systeme
 * (etape [5] de la chaine de traitement, regles R5 et R7).
 *
 * <p>Quatre familles de motifs sont recherchees, conformement au chapitre 8 : les
 * imperatifs adresses a un systeme, les marqueurs de role, les balises de cloture de
 * consigne et les formulations d'evaluation autoreferentes. L'analyse est purement
 * lexicale et deterministe — le cahier des charges ecarte un classifieur entraine —
 * et s'applique a un texte deja normalise NFKC.</p>
 *
 * <p>Cette classe est le seul point de definition des motifs : la regle R5 (metadonnees
 * et commentaires) et la regle R7 (corps du document) appliquent la meme analyse, ce
 * qui garantit que les deux regles ne divergent jamais sur ce qu'est une instruction.</p>
 */
public final class InstructionAnalyzer {

    private InstructionAnalyzer() {
    }

    /** Recherche toutes les occurrences de toutes les familles, dans l'ordre des familles. */
    public static List<InstructionMatch> analyze(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<InstructionMatch> matches = new ArrayList<>();
        for (InstructionPattern pattern : InstructionPattern.values()) {
            for (var compiled : pattern.patterns()) {
                Matcher matcher = compiled.matcher(text);
                while (matcher.find()) {
                    matches.add(new InstructionMatch(pattern, matcher.start(), matcher.group()));
                }
            }
        }
        return matches;
    }

    /** Familles effectivement rencontrees dans un texte, sans doublon et dans l'ordre d'apparition. */
    public static List<InstructionPattern> patternsIn(String text) {
        return analyze(text).stream()
                .map(InstructionMatch::pattern)
                .distinct()
                .toList();
    }

    /** Indique si le texte contient au moins une occurrence d'une famille de motifs. */
    public static boolean containsInstruction(String text) {
        return !analyze(text).isEmpty();
    }
}
