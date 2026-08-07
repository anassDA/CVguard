package ma.emsi.cvguard.domain.rule.impl;

import java.util.ArrayList;
import java.util.List;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionRule;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * R2 — Contraste insuffisant (EF-04, chapitre 8).
 *
 * <p>Calcule le ratio de contraste WCAG entre la couleur effectivement percue du
 * glyphe et celle de son fond, et signale tout ratio inferieur au seuil (1,5:1 par
 * defaut). {@link TextFragment#contrastRatio()} compose deja l'opacite et
 * l'invisibilite declaree par un mecanisme natif du format ({@code w:vanish},
 * {@code Tr 3}) : cette regle n'a donc pas a distinguer le blanc sur blanc, le texte
 * transparent et le texte au mode de rendu invisible, qui produisent tous trois un
 * ratio de 1,0.</p>
 */
@Component
public class InsufficientContrastRule implements DetectionRule {

    @Override
    public RuleCode code() {
        return RuleCode.R2;
    }

    @Override
    public List<Detection> apply(RuleContext context) {
        double minContrastRatio = context.thresholds().minContrastRatio();

        List<Detection> detections = new ArrayList<>();
        for (TextFragment fragment : context.document().renderedFragments()) {
            if (fragment.isBlank()) {
                continue;
            }
            double contrastRatio = fragment.contrastRatio();
            if (contrastRatio >= minContrastRatio) {
                continue;
            }

            detections.add(Detection.of(RuleCode.R2)
                    .at(fragment)
                    .detail("contrastRatio", round(contrastRatio))
                    .detail("color", fragment.color().toHex())
                    .detail("background", fragment.background().toHex())
                    .detail("hidden", fragment.hidden())
                    .detail("alpha", fragment.alpha())
                    .build());
        }
        return detections;
    }

    private static double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }
}
