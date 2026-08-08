package ma.emsi.cvguard.application;

import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.model.BoundingBox;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.Severity;
import ma.emsi.cvguard.domain.model.Verdict;

/**
 * Rapport d'inspection (EF-11, assembleur de rapport).
 *
 * <p>Structure restituee par {@code POST /api/v1/inspect} : chaque detection reference
 * sa regle, sa gravite, sa localisation et les elements chiffres qui l'ont declenchee,
 * afin que le verdict reste explicable (ENF-10). Les extraits sont tronques a la
 * longueur configuree pour ne pas rendre le rapport lui-meme exploitable.</p>
 *
 * @param documentId      identifiant de correlation de l'inspection (EF-14)
 * @param filename        nom de fichier declare par l'appelant
 * @param format          format reel du document
 * @param pageCount       nombre de pages, ou 0 si la pagination est inconnue
 * @param fragmentCount   nombre de fragments extraits
 * @param verdict         verdict deduit du score
 * @param riskScore       score de risque agrege, borne a 100
 * @param detections      signalements produits, dans l'ordre d'execution des regles
 * @param contributions   contribution de chaque regle au score
 * @param occurrences     nombre de detections par regle
 * @param ocrControl      resultat du controle OCR, ou {@code null} s'il n'a pas ete exerce
 * @param ruleFailures    regles ayant echoue, sans interrompre le traitement (ENF-06)
 * @param sanitizedLength longueur du texte assaini, hors delimiteurs
 * @param removedSegments nombre de segments purges par la politique d'assainissement
 * @param processingTimeMs duree totale du traitement
 */
public record InspectionReport(
        String documentId,
        String filename,
        String format,
        int pageCount,
        int fragmentCount,
        Verdict verdict,
        int riskScore,
        List<DetectionView> detections,
        Map<String, Integer> contributions,
        Map<String, Integer> occurrences,
        OcrControlView ocrControl,
        List<String> ruleFailures,
        int sanitizedLength,
        int removedSegments,
        long processingTimeMs) {

    /** Vue d'une detection dans le rapport. */
    public record DetectionView(
            String rule,
            String label,
            Severity severity,
            int page,
            float[] boundingBox,
            String excerpt,
            int fragmentIndex,
            Map<String, Object> details) {

        public static DetectionView of(Detection detection, int maxExcerptLength) {
            return new DetectionView(
                    detection.rule().name(),
                    detection.label(),
                    detection.severity(),
                    detection.page(),
                    detection.boundingBox() == null ? null : detection.boundingBox().toCoordinates(),
                    truncate(detection.excerpt(), maxExcerptLength),
                    detection.fragmentIndex(),
                    detection.details());
        }

        private static String truncate(String excerpt, int maxLength) {
            if (excerpt == null || excerpt.length() <= maxLength) {
                return excerpt;
            }
            return excerpt.substring(0, maxLength) + " [...]";
        }
    }

    /** Resultat du controle OCR conditionnel (etape [4], regle R6). */
    public record OcrControlView(
            boolean performed,
            Double similarity,
            int nativeTextLength,
            int ocrTextLength) {

        public OcrControlView withSimilarity(Double value) {
            return new OcrControlView(performed, value, nativeTextLength, ocrTextLength);
        }
    }
}
