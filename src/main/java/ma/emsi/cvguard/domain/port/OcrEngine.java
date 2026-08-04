package ma.emsi.cvguard.domain.port;

import java.awt.image.BufferedImage;

/**
 * Port de reconnaissance optique de caracteres (etape [4], regle R6).
 *
 * <p>La regle R6 compare le texte extrait nativement au texte reellement visible sur
 * la page rendue : tout ecart significatif revele du contenu present dans le fichier
 * mais invisible a l'oeil.</p>
 *
 * <p>Ce port est le point d'isolation prevu par l'analyse de risque : l'integration de
 * Tesseract est la plus incertaine du projet, et le service doit rester fonctionnel et
 * demontrable si elle n'aboutit pas. Une implementation indisponible se declare telle
 * via {@link #isAvailable()} plutot que d'echouer a l'execution.</p>
 */
public interface OcrEngine {

    /**
     * Indique si le moteur est operationnel — bibliotheque native chargee et donnees
     * de langue presentes. Lorsque la reponse est negative, la regle R6 se retire au
     * lieu d'echouer.
     */
    boolean isAvailable();

    /**
     * Reconnait le texte present sur une image de page.
     *
     * @param page image rendue de la page
     * @return le texte reconnu, jamais {@code null}
     * @throws OcrException si la reconnaissance echoue
     */
    String recognize(BufferedImage page);
}
