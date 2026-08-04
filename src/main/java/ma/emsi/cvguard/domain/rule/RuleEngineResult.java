package ma.emsi.cvguard.domain.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;

/**
 * Resultat d'une passe du moteur de regles.
 *
 * @param detections   signalements produits, dans l'ordre d'execution des regles
 * @param failures     regles ayant echoue sans interrompre le traitement
 * @param executedRules regles reellement executees, par opposition a celles qui etaient
 *                      desactivees ou inapplicables
 */
public record RuleEngineResult(
        List<Detection> detections,
        List<RuleFailure> failures,
        Set<RuleCode> executedRules) {

    public RuleEngineResult {
        detections = detections == null ? List.of() : List.copyOf(detections);
        failures = failures == null ? List.of() : List.copyOf(failures);
        // Set.copyOf ne garantit pas l'ordre d'iteration : l'ordre d'execution des regles
        // est une information de tracabilite, il est donc preserve explicitement.
        executedRules = executedRules == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(executedRules));
    }

    public static RuleEngineResult empty() {
        return new RuleEngineResult(List.of(), List.of(), Set.of());
    }

    public boolean hasDetections() {
        return !detections.isEmpty();
    }

    /**
     * Fusionne deux passes successives. La chaine de traitement execute le moteur en
     * plusieurs temps — regles structurelles, puis controle OCR conditionnel, puis
     * analyse d'instruction — et les resultats doivent etre agreges avant le calcul
     * du score.
     */
    public RuleEngineResult merge(RuleEngineResult other) {
        List<Detection> mergedDetections = new ArrayList<>(this.detections);
        mergedDetections.addAll(other.detections);

        List<RuleFailure> mergedFailures = new ArrayList<>(this.failures);
        mergedFailures.addAll(other.failures);

        Set<RuleCode> mergedRules = new LinkedHashSet<>(this.executedRules);
        mergedRules.addAll(other.executedRules);

        return new RuleEngineResult(mergedDetections, mergedFailures, mergedRules);
    }
}
