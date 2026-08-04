package ma.emsi.cvguard.exposition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ma.emsi.cvguard.config.RuleRegistry;
import ma.emsi.cvguard.domain.model.RuleCode;

/**
 * Vue d'une regle exposee par {@code GET /api/v1/rules}.
 *
 * @param code          code de la regle
 * @param label         libelle lisible
 * @param enabled       etat d'activation courant
 * @param weight        ponderation courante
 * @param defaultWeight ponderation prevue par le cahier des charges
 * @param implemented   indique si la regle dispose d'une implementation active dans
 *                      cette version du service
 */
@Schema(name = "Rule", description = "Etat d'une regle de detection")
public record RuleView(
        String code,
        String label,
        boolean enabled,
        int weight,
        int defaultWeight,
        boolean implemented) {

    public static RuleView of(RuleCode code, RuleRegistry.RuleState state, boolean implemented) {
        return new RuleView(
                code.name(),
                code.label(),
                state.enabled(),
                state.weight(),
                code.defaultWeight(),
                implemented);
    }
}
