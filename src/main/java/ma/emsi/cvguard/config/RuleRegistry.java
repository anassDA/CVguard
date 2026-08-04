package ma.emsi.cvguard.config;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.rule.RuleSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Etat courant des regles : activation et ponderation, modifiables a chaud (UC5, UC6).
 *
 * <p>Le service etant sans etat vis-a-vis des documents, ce registre est la seule
 * donnee qu'il conserve en memoire. Il est initialise depuis la configuration au
 * demarrage, puis pilotable par l'API d'administration. Les modifications ne sont pas
 * persistees : un redemarrage restitue la configuration declaree.</p>
 */
@Component
public class RuleRegistry implements RuleSettings {

    private static final Logger log = LoggerFactory.getLogger(RuleRegistry.class);

    private final Map<RuleCode, RuleState> states = Collections.synchronizedMap(new EnumMap<>(RuleCode.class));

    public RuleRegistry(CvGuardProperties properties) {
        Map<String, CvGuardProperties.RuleConfig> declared = properties.getDetection().getRules();
        for (RuleCode code : RuleCode.values()) {
            CvGuardProperties.RuleConfig config = lookup(declared, code);
            boolean enabled = config == null || config.isEnabled();
            int weight = config != null && config.getWeight() != null
                    ? config.getWeight()
                    : code.defaultWeight();
            states.put(code, new RuleState(enabled, weight));
        }
        log.info("Moteur de detection initialise : {}", states);
    }

    @Override
    public boolean isEnabled(RuleCode code) {
        RuleState state = states.get(code);
        return state != null && state.enabled();
    }

    @Override
    public int weight(RuleCode code) {
        RuleState state = states.get(code);
        return state == null ? code.defaultWeight() : state.weight();
    }

    public RuleState state(RuleCode code) {
        return states.get(code);
    }

    /** Vue immuable de l'etat courant, pour {@code GET /api/v1/rules}. */
    public Map<RuleCode, RuleState> snapshot() {
        synchronized (states) {
            return Map.copyOf(states);
        }
    }

    /**
     * Applique une modification partielle : un parametre non fourni reste inchange.
     *
     * @throws IllegalArgumentException si le poids demande est negatif
     */
    public RuleState update(RuleCode code, Boolean enabled, Integer weight) {
        if (weight != null && weight < 0) {
            throw new IllegalArgumentException("Le poids d'une regle ne peut etre negatif : " + weight);
        }
        synchronized (states) {
            RuleState current = states.getOrDefault(code, new RuleState(true, code.defaultWeight()));
            RuleState updated = new RuleState(
                    enabled == null ? current.enabled() : enabled,
                    weight == null ? current.weight() : weight);
            states.put(code, updated);
            log.info("Regle {} modifiee : {} -> {}", code, current, updated);
            return updated;
        }
    }

    /**
     * Reglage d'une regle.
     *
     * @param enabled etat d'activation
     * @param weight  ponderation utilisee par l'agregateur de score
     */
    public record RuleState(boolean enabled, int weight) {
    }

    /**
     * Recherche tolerante a la casse : la liaison des cles de map de Spring Boot ne
     * garantit pas la casse d'origine selon la source de configuration utilisee.
     */
    private static CvGuardProperties.RuleConfig lookup(
            Map<String, CvGuardProperties.RuleConfig> declared, RuleCode code) {
        if (declared == null || declared.isEmpty()) {
            return null;
        }
        CvGuardProperties.RuleConfig exact = declared.get(code.name());
        if (exact != null) {
            return exact;
        }
        return declared.entrySet().stream()
                .filter(entry -> entry.getKey() != null
                        && entry.getKey().toUpperCase(Locale.ROOT).equals(code.name()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
