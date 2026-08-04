package ma.emsi.cvguard.domain.port;

/**
 * Signale qu'un document n'a pu etre ouvert ou parcouru.
 *
 * <p>Contrairement a l'echec d'une regle, qui est absorbe par le moteur (ENF-06),
 * l'echec d'extraction est terminal : sans texte, aucune regle n'a d'objet. Le service
 * refuse alors le document plutot que de le declarer sain.</p>
 */
public class DocumentExtractionException extends RuntimeException {

    public DocumentExtractionException(String message) {
        super(message);
    }

    public DocumentExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
