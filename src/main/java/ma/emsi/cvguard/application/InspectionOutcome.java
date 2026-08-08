package ma.emsi.cvguard.application;

import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.Verdict;

/**
 * Produit d'une analyse complete d'un document.
 *
 * <p>Le rapport d'inspection et la version assainie sont construits dans le meme
 * traitement : le demonstrateur comparatif a besoin du texte natif brut et du contenu
 * assaini, et la construction du rapport a besoin du contenu assaini pour annoncer sa
 * longueur (EF-12, chapitre 8.2). Cette structure transporte donc les trois faces du
 * meme travail pour eviter d'extraire deux fois le document.</p>
 *
 * @param document document extrait, source du texte natif
 * @param report   rapport d'inspection expose par l'API
 * @param sanitized contenu assaini pret pour le modele (ENF-03)
 */
public record InspectionOutcome(
        ExtractedDocument document,
        InspectionReport report,
        SanitizedContent sanitized) {

    public Verdict verdict() {
        return report.verdict();
    }
}
