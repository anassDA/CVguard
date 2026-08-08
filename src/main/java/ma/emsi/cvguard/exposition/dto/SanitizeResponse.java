package ma.emsi.cvguard.exposition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ma.emsi.cvguard.domain.model.Verdict;

/**
 * Reponse de {@code POST /api/v1/sanitize} : la version assainie du document,
 * encapsulee dans des delimiteurs explicites, accompagnee de la consigne systeme que
 * l'appelant doit adjoindre a son propre appel au modele (chapitre 8.2, EF-11).
 *
 * @param documentId       identifiant de correlation de l'inspection
 * @param filename         nom de fichier declare par l'appelant
 * @param format           format reel du document
 * @param verdict          verdict de l'inspection associee
 * @param riskScore        score de risque associe
 * @param removedSegments  nombre de segments purges
 * @param envelopeTag      nom de la balise encapsulant le contenu
 * @param systemInstruction consigne systeme recommandee (chapitre 8.2)
 * @param sanitizedContent contenu assaini, pret pour le modele (ENF-03)
 */
@Schema(name = "SanitizeResponse", description = "Contenu assaini et pret pour un modele de langage")
public record SanitizeResponse(
        String documentId,
        String filename,
        String format,
        Verdict verdict,
        int riskScore,
        int removedSegments,
        String envelopeTag,
        String systemInstruction,
        String sanitizedContent) {
}
