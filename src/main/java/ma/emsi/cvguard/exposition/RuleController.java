package ma.emsi.cvguard.exposition;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import ma.emsi.cvguard.config.RuleRegistry;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.rule.RuleEngine;
import ma.emsi.cvguard.exposition.dto.RuleUpdateRequest;
import ma.emsi.cvguard.exposition.dto.RuleView;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration du moteur de detection (UC5, UC6).
 *
 * <p>Les modifications prennent effet immediatement et valent pour les inspections
 * suivantes. Elles ne sont pas persistees : le service reste sans etat au sens des
 * documents traites, et un redemarrage restitue la configuration declaree.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/rules", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Regles", description = "Consultation et parametrage des regles de detection")
public class RuleController {

    private final RuleRegistry registry;
    private final RuleEngine engine;

    public RuleController(RuleRegistry registry, RuleEngine engine) {
        this.registry = registry;
        this.engine = engine;
    }

    @GetMapping
    @Operation(
            summary = "Lister les regles",
            description = "Retourne le catalogue des regles, leur etat d'activation, leur ponderation "
                    + "courante et l'indication de leur mise en oeuvre effective dans cette version.")
    public List<RuleView> list() {
        Set<RuleCode> implemented = EnumSet.noneOf(RuleCode.class);
        implemented.addAll(engine.availableRules());

        return EnumSet.allOf(RuleCode.class).stream()
                .map(code -> RuleView.of(code, registry.state(code), implemented.contains(code)))
                .toList();
    }

    @PutMapping(value = "/{code}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Modifier une regle",
            description = "Active ou desactive une regle et ajuste sa ponderation. "
                    + "Les champs omis conservent leur valeur courante.")
    public RuleView update(@PathVariable String code, @Valid @RequestBody RuleUpdateRequest request) {
        RuleCode ruleCode = RuleCode.from(code)
                .orElseThrow(() -> new UnknownRuleException(code));
        RuleRegistry.RuleState state = registry.update(ruleCode, request.enabled(), request.weight());
        return RuleView.of(ruleCode, state, engine.availableRules().contains(ruleCode));
    }

    /** Code de regle inconnu dans l'URL. */
    public static class UnknownRuleException extends RuntimeException {

        public UnknownRuleException(String code) {
            super("Regle inconnue : " + code + ". Codes valides : " + EnumSet.allOf(RuleCode.class));
        }
    }
}
