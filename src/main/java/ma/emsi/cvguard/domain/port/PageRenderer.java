package ma.emsi.cvguard.domain.port;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Port de rendu graphique des pages, prealable au controle OCR de la regle R6.
 *
 * <p>Le rendu est l'operation qui rend l'attaque visible : un texte blanc sur blanc
 * ou masque sous une image est present dans le flux d'extraction mais absent de
 * l'image produite. C'est precisement cet ecart que R6 mesure.</p>
 */
public interface PageRenderer {

    /** Indique si le format soumis peut etre rendu graphiquement. */
    boolean canRender(byte[] content);

    /**
     * Rend chaque page du document sous forme d'image.
     *
     * @param content contenu binaire du document
     * @param dpi     resolution de rendu ; un compromis entre fidelite de l'OCR et cout
     * @return une image par page, dans l'ordre du document
     */
    List<BufferedImage> render(byte[] content, int dpi);
}
