package ma.emsi.cvguard.domain.rule.impl;

import java.util.List;
import java.util.Optional;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.rule.DetectionRule;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Component;

/**
 * R6 — Ecart texte extrait / texte rendu (EF-12, chapitre 8).
 *
 * <p>Le rendu graphique d'une page puis sa reconnaissance optique restituent ce qui est
 * reellement visible a l'ecran : un texte blanc sur blanc, transparent ou masque sous
 * un aplat est present dans le flux d'extraction mais absent de l'image produite.
 * La similarite Jaro-Winkler entre le texte natif et le texte OCR quantifie cet ecart ;
 * une divergence superieure au seuil (15 % par defaut) revele du contenu non visible.</p>
 *
 * <p>Cette regle ne declenche aucun travail elle-meme : le texte OCR est depose dans
 * l'espace de faits partages par l'orchestrateur d'inspection (etape [4], declenchee
 * uniquement lorsqu'un premier signal a ete leve, ENF-02). Lorsque le controle OCR n'a
 * pas ete exerce — moteur indisponible, format non rendable, ou aucun signal prealable —
 * la regle se retire via {@link #supports(RuleContext)} plutot que d'echouer.</p>
 */
@Component
public class OcrDiscrepancyRule implements DetectionRule {

    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();

    @Override
    public RuleCode code() {
        return RuleCode.R6;
    }

    @Override
    public boolean supports(RuleContext context) {
        return context.fact(RuleContext.FACT_OCR_TEXT, String.class).isPresent();
    }

    @Override
    public List<Detection> apply(RuleContext context) {
        Optional<String> ocrText = context.fact(RuleContext.FACT_OCR_TEXT, String.class);
        if (ocrText.isEmpty()) {
            return List.of();
        }

        String nativeText = context.document().nativeText();
        double similarity = JARO_WINKLER.apply(nativeText, ocrText.get());
        context.putFact(RuleContext.FACT_OCR_SIMILARITY, similarity);

        double gap = 1d - similarity;
        if (gap <= context.thresholds().ocrSimilarityGap()) {
            return List.of();
        }

        return List.of(Detection.of(RuleCode.R6)
                .detail("similarity", round(similarity))
                .detail("gap", round(gap))
                .detail("threshold", context.thresholds().ocrSimilarityGap())
                .detail("nativeTextLength", nativeText.length())
                .detail("ocrTextLength", ocrText.get().length())
                .build());
    }

    private static double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }
}
