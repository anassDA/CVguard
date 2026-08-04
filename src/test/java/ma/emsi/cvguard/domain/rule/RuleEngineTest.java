package ma.emsi.cvguard.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.RuleCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification des garanties structurelles du moteur : isolation des echecs (ENF-06),
 * respect de l'etat d'activation (UC5) et execution par etapes.
 */
class RuleEngineTest {

    private final RuleContext context = new RuleContext(
            new ExtractedDocument("test", DocumentFormat.PDF, "cv.pdf", List.of(), List.of(), Map.of()),
            DetectionThresholds.defaults());

    @Test
    @DisplayName("Une regle en echec n'interrompt pas la passe et est signalee")
    void failingRuleDoesNotStopThePipeline() {
        RuleEngine engine = new RuleEngine(
                List.of(
                        failing(RuleCode.R1, "extraction de police indisponible"),
                        producing(RuleCode.R7, 2)),
                RuleSettings.defaults());

        RuleEngineResult result = engine.run(context);

        assertThat(result.detections()).hasSize(2);
        assertThat(result.failures())
                .singleElement()
                .satisfies(failure -> {
                    assertThat(failure.rule()).isEqualTo(RuleCode.R1);
                    assertThat(failure.reason()).contains("extraction de police indisponible");
                });
        assertThat(result.executedRules()).containsExactly(RuleCode.R7);
    }

    @Test
    @DisplayName("Une regle desactivee n'est pas executee")
    void disabledRuleIsSkipped() {
        RuleEngine engine = new RuleEngine(
                List.of(producing(RuleCode.R1, 3), producing(RuleCode.R7, 1)),
                settingsDisabling(RuleCode.R1));

        RuleEngineResult result = engine.run(context);

        assertThat(result.detections()).hasSize(1);
        assertThat(result.executedRules()).containsExactly(RuleCode.R7);
    }

    @Test
    @DisplayName("Une regle inapplicable au document se retire sans erreur")
    void unsupportedRuleWithdrawsQuietly() {
        DetectionRule positional = new DetectionRule() {

            @Override
            public RuleCode code() {
                return RuleCode.R3;
            }

            @Override
            public boolean supports(RuleContext ignored) {
                return false;
            }

            @Override
            public List<Detection> apply(RuleContext ignored) {
                throw new AssertionError("La regle ne devait pas etre appliquee");
            }
        };

        RuleEngineResult result = new RuleEngine(List.of(positional), RuleSettings.defaults()).run(context);

        assertThat(result.detections()).isEmpty();
        assertThat(result.failures()).isEmpty();
        assertThat(result.executedRules()).isEmpty();
    }

    @Test
    @DisplayName("La selection permet d'executer le moteur par etapes")
    void selectionRestrictsExecutedRules() {
        RuleEngine engine = new RuleEngine(
                List.of(producing(RuleCode.R1, 1), producing(RuleCode.R6, 1), producing(RuleCode.R7, 1)),
                RuleSettings.defaults());

        RuleEngineResult structural = engine.run(context, EnumSet.of(RuleCode.R1));
        RuleEngineResult semantic = engine.run(context, EnumSet.of(RuleCode.R7));

        assertThat(structural.executedRules()).containsExactly(RuleCode.R1);
        assertThat(semantic.executedRules()).containsExactly(RuleCode.R7);

        RuleEngineResult merged = structural.merge(semantic);
        assertThat(merged.detections()).hasSize(2);
        assertThat(merged.executedRules()).containsExactly(RuleCode.R1, RuleCode.R7);
    }

    @Test
    @DisplayName("Les regles sont executees dans l'ordre de leur code")
    void rulesRunInCodeOrder() {
        RuleEngine engine = new RuleEngine(
                List.of(producing(RuleCode.R7, 1), producing(RuleCode.R1, 1), producing(RuleCode.R4, 1)),
                RuleSettings.defaults());

        assertThat(engine.availableRules()).containsExactly(RuleCode.R1, RuleCode.R4, RuleCode.R7);
    }

    private static DetectionRule producing(RuleCode code, int count) {
        return new DetectionRule() {

            @Override
            public RuleCode code() {
                return code;
            }

            @Override
            public List<Detection> apply(RuleContext ignored) {
                return java.util.stream.IntStream.range(0, count)
                        .mapToObj(index -> Detection.of(code).excerpt("detection " + index).build())
                        .toList();
            }
        };
    }

    private static DetectionRule failing(RuleCode code, String reason) {
        return new DetectionRule() {

            @Override
            public RuleCode code() {
                return code;
            }

            @Override
            public List<Detection> apply(RuleContext ignored) {
                throw new IllegalStateException(reason);
            }
        };
    }

    private static RuleSettings settingsDisabling(RuleCode disabled) {
        return new RuleSettings() {

            @Override
            public boolean isEnabled(RuleCode code) {
                return code != disabled;
            }

            @Override
            public int weight(RuleCode code) {
                return code.defaultWeight();
            }
        };
    }
}
