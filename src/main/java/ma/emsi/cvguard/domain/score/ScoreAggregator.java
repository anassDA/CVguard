package ma.emsi.cvguard.domain.score;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.Verdict;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleSettings;

/**
 * Agregation des signalements en un score de risque et un verdict (chapitre 8.1).
 *
 * <pre>
 * score = min(100, SOMME sur les regles declenchees de : poids(R) * (1 + log2(occurrences(R))))
 * </pre>
 *
 * <p>La croissance logarithmique est le point cle : une regle declenchee dix fois ne
 * pese pas dix fois son poids. Un CV comportant de nombreuses mentions en petits
 * caracteres — cas legitime frequent — ne doit pas etre condamne par la seule
 * repetition d'un signal faible. C'est la concordance de regles differentes, et non la
 * repetition de la meme, qui fait monter le score.</p>
 */
public class ScoreAggregator {

    private static final double LOG_2 = Math.log(2d);
    private static final int MAX_SCORE = 100;

    private final RuleSettings settings;
    private final DetectionThresholds thresholds;

    public ScoreAggregator(RuleSettings settings, DetectionThresholds thresholds) {
        this.settings = settings;
        this.thresholds = thresholds;
    }

    public RiskAssessment aggregate(Collection<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return RiskAssessment.safe();
        }

        Map<RuleCode, Integer> occurrences = new EnumMap<>(RuleCode.class);
        for (Detection detection : detections) {
            occurrences.merge(detection.rule(), 1, Integer::sum);
        }

        Map<RuleCode, Integer> contributions = new EnumMap<>(RuleCode.class);
        double total = 0d;
        for (Map.Entry<RuleCode, Integer> entry : occurrences.entrySet()) {
            RuleCode rule = entry.getKey();
            int weight = settings.weight(rule);
            if (weight <= 0) {
                continue;
            }
            double contribution = weight * (1d + log2(entry.getValue()));
            contributions.put(rule, (int) Math.round(contribution));
            total += contribution;
        }

        int score = (int) Math.min(MAX_SCORE, Math.round(total));
        return new RiskAssessment(score, verdictFor(score), contributions, occurrences);
    }

    /**
     * Traduction du score en verdict.
     *
     * <p>Le palier SUSPECT n'est pas une commodite : il garantit qu'aucune candidature
     * n'est ecartee par la seule decision du systeme dans la zone d'incertitude.</p>
     */
    public Verdict verdictFor(int score) {
        if (score >= thresholds.maliciousScore()) {
            return Verdict.MALICIOUS;
        }
        if (score >= thresholds.suspectScore()) {
            return Verdict.SUSPECT;
        }
        return Verdict.SAFE;
    }

    private static double log2(int occurrences) {
        return Math.log(occurrences) / LOG_2;
    }
}
