package ma.emsi.cvguard.domain.model;

/**
 * Boite englobante d'un fragment de texte, exprimee en points PDF dans le repere
 * de la page (origine en bas a gauche, axe Y ascendant).
 *
 * <p>Support de la regle R3 : positionnement hors de la zone visible.</p>
 *
 * @param x      abscisse du coin inferieur gauche
 * @param y      ordonnee du coin inferieur gauche
 * @param width  largeur (peut etre nulle pour un fragment vide)
 * @param height hauteur
 */
public record BoundingBox(float x, float y, float width, float height) {

    /** Construit une boite a partir de deux coins opposes, dans un ordre quelconque. */
    public static BoundingBox ofCorners(float x1, float y1, float x2, float y2) {
        float left = Math.min(x1, x2);
        float bottom = Math.min(y1, y2);
        return new BoundingBox(left, bottom, Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    public float left() {
        return x;
    }

    public float bottom() {
        return y;
    }

    public float right() {
        return x + width;
    }

    public float top() {
        return y + height;
    }

    public float area() {
        return width * height;
    }

    /** Indique si cette boite est entierement contenue dans {@code outer}, a la tolerance pres. */
    public boolean containedIn(BoundingBox outer, float tolerance) {
        return left() >= outer.left() - tolerance
                && bottom() >= outer.bottom() - tolerance
                && right() <= outer.right() + tolerance
                && top() <= outer.top() + tolerance;
    }

    public boolean intersects(BoundingBox other) {
        return left() < other.right()
                && right() > other.left()
                && bottom() < other.top()
                && top() > other.bottom();
    }

    /** Aire de l'intersection avec {@code other}, nulle si les boites sont disjointes. */
    public float intersectionArea(BoundingBox other) {
        float overlapWidth = Math.min(right(), other.right()) - Math.max(left(), other.left());
        float overlapHeight = Math.min(top(), other.top()) - Math.max(bottom(), other.bottom());
        if (overlapWidth <= 0f || overlapHeight <= 0f) {
            return 0f;
        }
        return overlapWidth * overlapHeight;
    }

    /**
     * Serialisation attendue par le rapport JSON (EF-11) : {@code [x1, y1, x2, y2]}.
     */
    public float[] toCoordinates() {
        return new float[]{left(), bottom(), right(), top()};
    }
}
