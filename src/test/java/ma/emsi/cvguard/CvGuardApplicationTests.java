package ma.emsi.cvguard;

import static org.assertj.core.api.Assertions.assertThat;

import ma.emsi.cvguard.config.RuleRegistry;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleEngine;
import ma.emsi.cvguard.domain.score.ScoreAggregator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verification du cablage : le contexte demarre et les composants du domaine sont
 * assembles a partir de la configuration declaree.
 */
@SpringBootTest
class CvGuardApplicationTests {

    @Autowired
    private RuleRegistry registry;

    @Autowired
    private RuleEngine engine;

    @Autowired
    private ScoreAggregator aggregator;

    @Autowired
    private DetectionThresholds thresholds;

    @Test
    @DisplayName("Le contexte applicatif demarre et expose les composants du domaine")
    void contextLoads() {
        assertThat(registry).isNotNull();
        assertThat(engine).isNotNull();
        assertThat(aggregator).isNotNull();
    }

    @Test
    @DisplayName("Les ponderations declarees en configuration sont celles du cahier des charges")
    void weightsMatchSpecification() {
        for (RuleCode code : RuleCode.values()) {
            assertThat(registry.weight(code))
                    .as("poids de %s", code)
                    .isEqualTo(code.defaultWeight());
            assertThat(registry.isEnabled(code)).as("activation de %s", code).isTrue();
        }
    }

    @Test
    @DisplayName("Les seuils declares en configuration sont ceux du chapitre 8")
    void thresholdsMatchSpecification() {
        assertThat(thresholds.minFontSizePt()).isEqualTo(3f);
        assertThat(thresholds.minContrastRatio()).isEqualTo(1.5d);
        assertThat(thresholds.suspectScore()).isEqualTo(25);
        assertThat(thresholds.maliciousScore()).isEqualTo(60);
    }
}
