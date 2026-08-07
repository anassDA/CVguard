package ma.emsi.cvguard.domain.rule.impl;

import java.util.ArrayList;
import java.util.List;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.PageGeometry;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionRule;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * R3 — Positionnement hors zone (EF-05, chapitre 8), volet hors-page.
 *
 * <p>Signale tout fragment dont la boite englobante deborde de la zone visible de la
 * page ({@code cropBox}), a une tolerance pres qui absorbe les debordements
 * typographiques legitimes (ascendantes, empattements).</p>
 *
 * <p><b>Perimetre de cette version.</b> Le second volet de R3 defini par le cahier des
 * charges — un fragment "recouvert par un element graphique opaque" — n'est pas
 * couvert ici. Le detecter correctement suppose de connaitre l'ordre de peinture
 * relatif du texte et des elements graphiques du flux de contenu : un simple
 * recouvrement de boites englobantes, sans cet ordre, confondrait un texte
 * dissimule sous un aplat avec un texte parfaitement visible devant un bandeau de
 * couleur — un cas frequent et legitime dans un CV (en-tete de section, encadre).
 * Cette confusion irait a l'encontre du principe du chapitre 10 : un systeme qui
 * ecarte des candidats legitimes est plus nuisible que l'attaque qu'il previent.
 * Cette extension est documentee comme perspective d'evolution plutot
 * qu'implementee au risque d'introduire des faux positifs non maitrises.</p>
 *
 * <p>Ne s'applique qu'au PDF : un DOCX n'a pas de geometrie de page connue
 * (chapitre 7.4), et {@link #supports(RuleContext)} retire la regle en consequence.</p>
 */
@Component
public class OutOfPageAreaRule implements DetectionRule {

    @Override
    public RuleCode code() {
        return RuleCode.R3;
    }

    @Override
    public boolean supports(RuleContext context) {
        return !context.document().pages().isEmpty();
    }

    @Override
    public List<Detection> apply(RuleContext context) {
        float tolerance = context.thresholds().positionTolerancePt();

        List<Detection> detections = new ArrayList<>();
        for (TextFragment fragment : context.document().fragments()) {
            if (fragment.isBlank() || !fragment.hasKnownPosition()) {
                continue;
            }
            PageGeometry page = context.document().page(fragment.page());
            if (page == null) {
                continue;
            }
            if (fragment.box().containedIn(page.visibleArea(), tolerance)) {
                continue;
            }

            detections.add(Detection.of(RuleCode.R3)
                    .at(fragment)
                    .detail("visibleArea", page.visibleArea().toCoordinates())
                    .detail("tolerancePt", tolerance)
                    .build());
        }
        return detections;
    }
}
