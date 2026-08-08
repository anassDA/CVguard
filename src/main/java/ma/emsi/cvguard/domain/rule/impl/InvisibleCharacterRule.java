package ma.emsi.cvguard.domain.rule.impl;

import java.util.ArrayList;
import java.util.List;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionRule;
import ma.emsi.cvguard.domain.rule.RuleContext;
import ma.emsi.cvguard.domain.text.InvisibleCharacterMatch;
import ma.emsi.cvguard.domain.text.InvisibleCharacterScanner;
import org.springframework.stereotype.Component;

/**
 * R4 — Caracteres invisibles ou de largeur nulle (chapitre 8).
 *
 * <p>Detecte les points de code de largeur nulle (U+200B, U+200C, U+200D, U+2060,
 * U+FEFF) et les marques directionnelles (U+200E, U+202A...) inseres entre les mots,
 * et signale tout fragment dont la densite de ces caracteres depasse le seuil
 * (1 % par defaut). Un caractere invisible isole, fortuit ou legitime — un espace
 * insécable pleine chasse n'en fait pas partie — ne suffit pas a declarer un fragment
 * suspect : c'est la densite anormale qui trahit la dissimulation deliberee.</p>
 *
 * <p>La liste des points de code releves est portee par {@link InvisibleCharacterScanner}
 * afin que la regle et la politique d'assainissement ne divergent jamais sur la
 * definition d'un caractere invisible.</p>
 */
@Component
public class InvisibleCharacterRule implements DetectionRule {

    @Override
    public RuleCode code() {
        return RuleCode.R4;
    }

    @Override
    public List<Detection> apply(RuleContext context) {
        double maxDensity = context.thresholds().invisibleCharDensity();

        List<Detection> detections = new ArrayList<>();
        for (TextFragment fragment : context.document().fragments()) {
            if (fragment.isBlank()) {
                continue;
            }
            List<InvisibleCharacterMatch> matches = InvisibleCharacterScanner.scan(fragment.text());
            if (matches.isEmpty()) {
                continue;
            }
            double density = (double) matches.size() / fragment.length();
            if (density <= maxDensity) {
                continue;
            }

            detections.add(Detection.of(RuleCode.R4)
                    .at(fragment)
                    .detail("count", matches.size())
                    .detail("density", round(density))
                    .detail("threshold", maxDensity)
                    .detail("samples", samples(matches))
                    .build());
        }
        return detections;
    }

    /** Restitue les points de code rencontres sous forme lisible, dans la limite de trois. */
    private static List<String> samples(List<InvisibleCharacterMatch> matches) {
        return matches.stream()
                .limit(3)
                .map(match -> match.hex() + " (" + match.name() + ")")
                .toList();
    }

    private static double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }
}
