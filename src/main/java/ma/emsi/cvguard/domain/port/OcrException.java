package ma.emsi.cvguard.domain.port;

/**
 * Signale l'echec d'une reconnaissance optique.
 *
 * <p>Cet echec est absorbe par le moteur de regles (ENF-06) : l'inspection se poursuit
 * sans le signal R6, et l'echec est mentionne dans le rapport.</p>
 */
public class OcrException extends RuntimeException {

    public OcrException(String message) {
        super(message);
    }

    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }
}
