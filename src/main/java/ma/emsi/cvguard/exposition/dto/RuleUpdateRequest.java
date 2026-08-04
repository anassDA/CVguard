package ma.emsi.cvguard.exposition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Corps de {@code PUT /api/v1/rules/{code}} (UC5, UC6).
 *
 * <p>Les deux champs sont facultatifs et traites independamment : fournir le seul
 * poids laisse l'etat d'activation inchange.</p>
 *
 * @param enabled nouvel etat d'activation, ou {@code null} pour le conserver
 * @param weight  nouvelle ponderation, ou {@code null} pour la conserver
 */
@Schema(name = "RuleUpdateRequest", description = "Modification de l'activation ou du poids d'une regle")
public record RuleUpdateRequest(
        Boolean enabled,

        @Min(value = 0, message = "Le poids ne peut etre negatif")
        @Max(value = 100, message = "Le poids ne peut exceder 100")
        Integer weight) {
}
