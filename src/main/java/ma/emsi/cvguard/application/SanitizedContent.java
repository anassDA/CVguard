package ma.emsi.cvguard.application;

/**
 * Resultat de la politique d'assainissement (etape [7] de la chaine de traitement).
 *
 * @param content        texte encapsule dans des delimiteurs explicites, pret a etre
 *                       transmis a un modele de langage
 * @param removedSegments nombre de segments purges (fragments incrimines par R1 a R7)
 * @param keptLength     longueur du texte conserves, hors delimiteurs
 */
public record SanitizedContent(String content, int removedSegments, int keptLength) {
}
