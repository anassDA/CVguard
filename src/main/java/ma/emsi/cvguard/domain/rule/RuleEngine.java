package ma.emsi.cvguard.domain.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrateur des regles de detection (etape [3] de la chaine de traitement).
 *
 * <p>Le moteur ne connait que le contrat {@link DetectionRule} : il recoit la liste
 * des implementations disponibles et les execute. Publier une nouvelle regle suffit
 * a l'integrer, sans modification de cette classe (ENF-07).</p>
 *
 * <p>Conformement a ENF-06, l'echec d'une regle est intercepte, consigne et restitue
 * dans le resultat, mais n'interrompt pas la passe : une regle defaillante ne doit pas
 * priver l'inspection des signaux leves par les six autres.</p>
 */
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<DetectionRule> rules;
    private final RuleSettings settings;

    public RuleEngine(Collection<DetectionRule> rules, RuleSettings settings) {
        this.settings = settings;
        this.rules = rules.stream()
                .sorted(Comparator.comparing(DetectionRule::code))
                .toList();
    }

    /** Execute l'integralite des regles disponibles. */
    public RuleEngineResult run(RuleContext context) {
        return run(context, EnumSet.allOf(RuleCode.class));
    }

    /**
     * Execute les seules regles dont le code figure dans {@code selection}.
     *
     * <p>La chaine de traitement procede par etapes : les regles structurelles d'abord,
     * le controle OCR ensuite et seulement si un premier signal a ete leve (ENF-02),
     * l'analyse d'instruction enfin. Cette signature permet a l'orchestrateur de piloter
     * ces etapes sans que le moteur ait a connaitre leur enchainement.</p>
     */
    public RuleEngineResult run(RuleContext context, Set<RuleCode> selection) {
        List<Detection> detections = new ArrayList<>();
        List<RuleFailure> failures = new ArrayList<>();
        Set<RuleCode> executed = new LinkedHashSet<>();

        for (DetectionRule rule : rules) {
            RuleCode code = rule.code();
            if (!selection.contains(code) || !settings.isEnabled(code)) {
                continue;
            }
            try {
                if (!rule.supports(context)) {
                    continue;
                }
                List<Detection> produced = rule.apply(context);
                executed.add(code);
                if (produced != null && !produced.isEmpty()) {
                    detections.addAll(produced);
                }
            } catch (RuntimeException failure) {
                log.warn("Regle {} en echec, le traitement se poursuit : {}", code, failure.toString());
                failures.add(new RuleFailure(code, describe(failure)));
            }
        }

        return new RuleEngineResult(detections, failures, executed);
    }

    /** Regles disponibles dans le contexte d'execution courant. */
    public List<RuleCode> availableRules() {
        return rules.stream().map(DetectionRule::code).toList();
    }

    private static String describe(RuntimeException failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName() + (message == null ? "" : " : " + message);
    }
}
