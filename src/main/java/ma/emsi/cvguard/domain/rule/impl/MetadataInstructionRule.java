package ma.emsi.cvguard.domain.rule.impl;

import java.util.ArrayList;
import java.util.List;
import ma.emsi.cvguard.domain.instruction.InstructionAnalyzer;
import ma.emsi.cvguard.domain.instruction.InstructionPattern;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionRule;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * R5 — Metadonnees et commentaires (EF-13, chapitre 8).
 *
 * <p>Les champs Title, Subject et Keywords d'un document, ainsi que ses commentaires,
 * sont rarement inspectes : un candidat peut y deposer une instruction que le parseur
 * transmettra au modele sans jamais l'avoir soumise a un quelconque controle. Cette
 * regle y applique la meme analyse d'instruction que la regle R7 — voir
 * {@link InstructionAnalyzer}, source unique des motifs.</p>
 *
 * <p>La seule presence de metadonnees ou de commentaires n'est pas un signal : un nom
 * d'auteur, un titre de CV ou un commentaire de relecture sont legitimes. Seule une
 * structure d'instruction dans ces champs est signalee, a la ponderation reduite
 * (10) qui correspond a la gravite du vecteur.</p>
 */
@Component
public class MetadataInstructionRule implements DetectionRule {

    @Override
    public RuleCode code() {
        return RuleCode.R5;
    }

    @Override
    public List<Detection> apply(RuleContext context) {
        List<Detection> detections = new ArrayList<>();
        for (TextFragment fragment : context.document().fragments()) {
            if (fragment.origin() != FragmentOrigin.METADATA
                    && fragment.origin() != FragmentOrigin.COMMENT) {
                continue;
            }
            List<InstructionPattern> patterns = InstructionAnalyzer.patternsIn(fragment.text());
            if (patterns.isEmpty()) {
                continue;
            }

            detections.add(Detection.of(RuleCode.R5)
                    .at(fragment)
                    .detail("origin", fragment.origin().name())
                    .detail("matchedPattern", patterns.get(0).name())
                    .detail("matchedPatterns", patterns.stream().map(InstructionPattern::name).toList())
                    .build());
        }
        return detections;
    }
}
