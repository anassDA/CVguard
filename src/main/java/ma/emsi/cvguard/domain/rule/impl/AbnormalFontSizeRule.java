package ma.emsi.cvguard.domain.rule.impl;

import java.util.ArrayList;
import java.util.List;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionRule;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * R1 — Corps typographique anormal (EF-03, chapitre 8).
 *
 * <p>Signale tout fragment dont le corps effectif est inferieur au seuil absolu
 * (3 pt par defaut) ou inferieur a une fraction du corps median du document (40 %
 * par defaut). Le second critere est necessaire en complement du premier : un
 * document dont le texte courant est deja compose en corps 6 rendrait un texte
 * dissimule en corps 5 indetectable par le seul seuil absolu.</p>
 *
 * <p>S'applique indifferemment au PDF et au DOCX : {@link TextFragment#isRendered()}
 * ne suppose aucune position connue, seulement des attributs typographiques.</p>
 */
@Component
public class AbnormalFontSizeRule implements DetectionRule {

    @Override
    public RuleCode code() {
        return RuleCode.R1;
    }

    @Override
    public List<Detection> apply(RuleContext context) {
        DetectionThresholds thresholds = context.thresholds();
        double medianFontSizePt = context.medianFontSizePt();
        double relativeFloor = medianFontSizePt * thresholds.medianFontRatio();

        List<Detection> detections = new ArrayList<>();
        for (TextFragment fragment : context.document().renderedFragments()) {
            if (fragment.isBlank()) {
                continue;
            }
            boolean belowAbsoluteFloor = fragment.fontSizePt() < thresholds.minFontSizePt();
            boolean belowRelativeFloor = relativeFloor > 0d && fragment.fontSizePt() < relativeFloor;
            if (!belowAbsoluteFloor && !belowRelativeFloor) {
                continue;
            }

            detections.add(Detection.of(RuleCode.R1)
                    .at(fragment)
                    .detail("fontSizePt", fragment.fontSizePt())
                    .detail("medianFontSizePt", round(medianFontSizePt))
                    .detail("belowAbsoluteFloor", belowAbsoluteFloor)
                    .detail("belowRelativeFloor", belowRelativeFloor)
                    .build());
        }
        return detections;
    }

    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
