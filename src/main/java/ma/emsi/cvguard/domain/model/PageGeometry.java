package ma.emsi.cvguard.domain.model;

/**
 * Geometrie d'une page, necessaire a la regle R3 pour decider si un fragment
 * sort de la zone effectivement imprimee.
 *
 * @param number   numero de page (1-based)
 * @param mediaBox surface physique declaree de la page
 * @param cropBox  surface effectivement affichee et imprimee ; c'est elle qui fait foi
 * @param rotation rotation de la page en degres (0, 90, 180 ou 270)
 */
public record PageGeometry(int number, BoundingBox mediaBox, BoundingBox cropBox, int rotation) {

    public PageGeometry {
        if (cropBox == null) {
            cropBox = mediaBox;
        }
    }

    /** Zone de reference pour la detection de positionnement hors zone. */
    public BoundingBox visibleArea() {
        return cropBox;
    }
}
