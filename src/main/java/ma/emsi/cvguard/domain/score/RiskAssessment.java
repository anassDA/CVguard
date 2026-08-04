package ma.emsi.cvguard.domain.score;

import java.util.Map;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.Verdict;

/**
 * Resultat de l'agregation (EF-10).
 *
 * @param score         score de risque agrege, borne a 100
 * @param verdict       verdict deduit du score
 * @param contributions contribution de chaque regle declenchee, restituee pour rendre
 *                      le score explicable (ENF-10)
 * @param occurrences   nombre de detections par regle
 */
public record RiskAssessment(
        int score,
        Verdict verdict,
        Map<RuleCode, Integer> contributions,
        Map<RuleCode, Integer> occurrences) {

    public RiskAssessment {
        contributions = contributions == null ? Map.of() : Map.copyOf(contributions);
        occurrences = occurrences == null ? Map.of() : Map.copyOf(occurrences);
    }

    public static RiskAssessment safe() {
        return new RiskAssessment(0, Verdict.SAFE, Map.of(), Map.of());
    }
}
