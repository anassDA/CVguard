package ma.emsi.cvguard.domain.rule;

import ma.emsi.cvguard.domain.model.RuleCode;

/**
 * Etat d'activation et ponderation des regles, vus par le moteur de detection.
 *
 * <p>Le domaine se contente de consulter ce contrat ; sa mise en oeuvre mutable,
 * qui rend les cas d'utilisation UC5 et UC6 possibles a chaud, releve de la couche
 * de configuration.</p>
 */
public interface RuleSettings {

    boolean isEnabled(RuleCode code);

    int weight(RuleCode code);

    /** Reglage de reference : toutes les regles actives, aux poids du cahier des charges. */
    static RuleSettings defaults() {
        return new RuleSettings() {

            @Override
            public boolean isEnabled(RuleCode code) {
                return true;
            }

            @Override
            public int weight(RuleCode code) {
                return code.defaultWeight();
            }
        };
    }
}
