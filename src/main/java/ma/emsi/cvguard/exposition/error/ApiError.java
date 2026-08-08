package ma.emsi.cvguard.exposition.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Corps de reponse uniforme en cas d'erreur.
 *
 * <p>Le message reste volontairement descriptif sans exposer d'element interne :
 * le service traite des fichiers non fiables, et une trace d'execution renvoyee a
 * l'appelant constituerait une fuite d'information.</p>
 *
 * @param timestamp    horodatage de l'erreur
 * @param status       code HTTP
 * @param error        libelle court
 * @param message      description destinee a l'appelant
 * @param correlationId identifiant de correlation, s'il a deja ete attribue (EF-14)
 * @param details      precisions, notamment les violations de validation
 */
@Schema(name = "ApiError", description = "Reponse d'erreur normalisee")
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String correlationId,
        List<String> details) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, null, List.of());
    }

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, null, details);
    }

    /** Variante portant l'identifiant de correlation de l'inspection en cours (EF-14). */
    public static ApiError of(int status, String error, String message, List<String> details, String correlationId) {
        return new ApiError(Instant.now(), status, error, message, correlationId, details);
    }
}
