package ma.emsi.cvguard.exposition.error;

import java.util.List;
import ma.emsi.cvguard.domain.port.DocumentExtractionException;
import ma.emsi.cvguard.exposition.RuleController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduction uniforme des erreurs en reponses HTTP.
 *
 * <p>Aucune trace d'execution n'est renvoyee a l'appelant : les entrees traitees sont
 * par nature non fiables, et le detail interne du traitement ne doit pas fuir vers un
 * eventuel attaquant. Le detail complet reste dans la journalisation du service.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuleController.UnknownRuleException.class)
    public ResponseEntity<ApiError> handleUnknownRule(RuleController.UnknownRuleException exception) {
        return build(HttpStatus.NOT_FOUND, "Regle inconnue", exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<String> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Requete invalide",
                "Le corps de la requete ne respecte pas le contrat attendu.", violations);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException exception) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Document trop volumineux",
                "La taille du document depasse la limite admise de 10 Mo.", List.of());
    }

    @ExceptionHandler(DocumentExtractionException.class)
    public ResponseEntity<ApiError> handleExtraction(DocumentExtractionException exception) {
        log.warn("Extraction impossible : {}", exception.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Document illisible",
                "Le document n'a pu etre ouvert ou parcouru. Un document illisible est refuse "
                        + "plutot que declare sain.",
                List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, "Requete invalide", exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Erreur inattendue lors du traitement de la requete", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Le traitement a echoue. Consultez la journalisation du service.", List.of());
    }

    private static ResponseEntity<ApiError> build(
            HttpStatus status, String error, String message, List<String> details) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), error, message, details));
    }
}
