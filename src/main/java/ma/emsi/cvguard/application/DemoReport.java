package ma.emsi.cvguard.application;

import ma.emsi.cvguard.domain.model.Verdict;

/**
 * Resultat du demonstrateur comparatif (EF-13, O6).
 *
 * @param documentId        identifiant de correlation de l'inspection
 * @param simulated         indique si la reponse du modele est simulee (aucun appel
 *                          reseau) ou issue d'un modele reellement interroge
 * @param filename          nom de fichier declare par l'appelant
 * @param format            format reel du document
 * @param verdict           verdict du pipeline protege, qui inspecte le document
 * @param riskScore         score de risque correspondant
 * @param naivePipeline     pipeline naif : texte brut transmis tel quel au modele
 * @param protectedPipeline pipeline protege : texte assaini et encapsule, seule
 *                          frontiere legitime vers un modele (ENF-03)
 */
public record DemoReport(
        String documentId,
        boolean simulated,
        String filename,
        String format,
        Verdict verdict,
        int riskScore,
        PipelineView naivePipeline,
        PipelineView protectedPipeline) {

    /** Vue d'une execution de pipeline : contenu envoye (longueur) et reponse du modele. */
    public record PipelineView(String kind, int contentLength, String response, String error) {
    }
}
