package ma.emsi.cvguard.domain.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.Verdict;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification de la formule d'agregation du chapitre 8.1.
 *
 * <pre>score = min(100, SOMME poids(R) * (1 + log2(occurrences(R))))</pre>
 */
class ScoreAggregatorTest {

    private final ScoreAggregator aggregator =
            new ScoreAggregator(RuleSettings.defaults(), DetectionThresholds.defaults());

    @Test
    @DisplayName("Aucune detection : score nul et verdict SAFE")
    void noDetectionYieldsSafe() {
        RiskAssessment assessment = aggregator.aggregate(List.of());

        assertThat(assessment.score()).isZero();
        assertThat(assessment.verdict()).isEqualTo(Verdict.SAFE);
        assertThat(assessment.contributions()).isEmpty();
    }

    @Test
    @DisplayName("Une occurrence contribue exactement du poids de la regle")
    void singleOccurrenceContributesItsWeight() {
        RiskAssessment assessment = aggregator.aggregate(detections(RuleCode.R7, 1));

        // log2(1) = 0, la contribution vaut donc 40 * (1 + 0)
        assertThat(assessment.score()).isEqualTo(40);
        assertThat(assessment.verdict()).isEqualTo(Verdict.SUSPECT);
        assertThat(assessment.contributions()).containsEntry(RuleCode.R7, 40);
        assertThat(assessment.occurrences()).containsEntry(RuleCode.R7, 1);
    }

    @Test
    @DisplayName("Deux occurrences doublent la contribution, quatre la triplent")
    void repetitionGrowsLogarithmically() {
        // 30 * (1 + log2(2)) = 60
        assertThat(aggregator.aggregate(detections(RuleCode.R2, 2)).score()).isEqualTo(60);
        // 30 * (1 + log2(4)) = 90
        assertThat(aggregator.aggregate(detections(RuleCode.R2, 4)).score()).isEqualTo(90);
    }

    @Test
    @DisplayName("La repetition d'un signal faible ne suffit pas a condamner un document")
    void repeatedWeakSignalStaysBelowRejection() {
        // Cas du CV comportant de nombreuses mentions legales en petits caracteres :
        // seize occurrences de la regle la moins ponderee restent sous le seuil de rejet.
        RiskAssessment assessment = aggregator.aggregate(detections(RuleCode.R5, 16));

        // 10 * (1 + log2(16)) = 50
        assertThat(assessment.score()).isEqualTo(50);
        assertThat(assessment.verdict()).isEqualTo(Verdict.SUSPECT);
    }

    @Test
    @DisplayName("La concordance de regles distinctes fait basculer le verdict")
    void concurringRulesEscalateTheVerdict() {
        List<Detection> mixed = new ArrayList<>();
        mixed.addAll(detections(RuleCode.R2, 1));
        mixed.addAll(detections(RuleCode.R7, 1));

        RiskAssessment assessment = aggregator.aggregate(mixed);

        assertThat(assessment.score()).isEqualTo(70);
        assertThat(assessment.verdict()).isEqualTo(Verdict.MALICIOUS);
        assertThat(assessment.contributions()).containsOnlyKeys(RuleCode.R2, RuleCode.R7);
    }

    @Test
    @DisplayName("Le score est borne a 100")
    void scoreIsCappedAtHundred() {
        List<Detection> many = new ArrayList<>();
        for (RuleCode code : RuleCode.values()) {
            many.addAll(detections(code, 8));
        }

        assertThat(aggregator.aggregate(many).score()).isEqualTo(100);
    }

    @Test
    @DisplayName("Une regle de poids nul est neutralisee sans etre desactivee")
    void zeroWeightRuleDoesNotContribute() {
        ScoreAggregator neutralised = new ScoreAggregator(new RuleSettings() {

            @Override
            public boolean isEnabled(RuleCode code) {
                return true;
            }

            @Override
            public int weight(RuleCode code) {
                return code == RuleCode.R7 ? 0 : code.defaultWeight();
            }
        }, DetectionThresholds.defaults());

        RiskAssessment assessment = neutralised.aggregate(detections(RuleCode.R7, 3));

        assertThat(assessment.score()).isZero();
        assertThat(assessment.verdict()).isEqualTo(Verdict.SAFE);
        assertThat(assessment.contributions()).isEmpty();
    }

    @Test
    @DisplayName("Les paliers de verdict suivent les seuils configures")
    void verdictThresholds() {
        assertThat(aggregator.verdictFor(0)).isEqualTo(Verdict.SAFE);
        assertThat(aggregator.verdictFor(24)).isEqualTo(Verdict.SAFE);
        assertThat(aggregator.verdictFor(25)).isEqualTo(Verdict.SUSPECT);
        assertThat(aggregator.verdictFor(59)).isEqualTo(Verdict.SUSPECT);
        assertThat(aggregator.verdictFor(60)).isEqualTo(Verdict.MALICIOUS);
        assertThat(aggregator.verdictFor(100)).isEqualTo(Verdict.MALICIOUS);
    }

    @Test
    @DisplayName("Supprimer la zone d'incertitude est refuse a la construction des seuils")
    void suspectBandCannotBeCollapsed() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new DetectionThresholds(3f, 0.4f, 1.5d, 2f, 0.9d, 0.01d, 0.15d, 60, 60));
    }

    private static List<Detection> detections(RuleCode code, int count) {
        List<Detection> detections = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            detections.add(Detection.of(code).excerpt("occurrence " + i).build());
        }
        return detections;
    }
}
