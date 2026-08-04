package ma.emsi.cvguard.domain.model;

import java.util.Locale;

/**
 * Couleur RVB dont chaque composante est normalisee dans l'intervalle [0, 1].
 *
 * <p>Support de la regle R2 : le calcul du ratio de contraste suit la formule
 * WCAG 2.1 (luminance relative), qui est la reference retenue par le cahier
 * des charges (EF-04, seuil de declenchement a 1,5:1).</p>
 */
public record RgbColor(float red, float green, float blue) {

    public static final RgbColor WHITE = new RgbColor(1f, 1f, 1f);
    public static final RgbColor BLACK = new RgbColor(0f, 0f, 0f);

    public RgbColor {
        red = clamp(red);
        green = clamp(green);
        blue = clamp(blue);
    }

    /** Construit une couleur a partir de composantes exprimees sur 8 bits. */
    public static RgbColor ofBytes(int red, int green, int blue) {
        return new RgbColor(red / 255f, green / 255f, blue / 255f);
    }

    /** Construit une couleur a partir d'un entier RVB compacte (0xRRGGBB). */
    public static RgbColor ofPacked(int rgb) {
        return ofBytes((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Luminance relative au sens de la WCAG 2.1.
     *
     * @return une valeur dans [0, 1], 0 pour le noir et 1 pour le blanc
     */
    public double relativeLuminance() {
        return 0.2126d * linearize(red) + 0.7152d * linearize(green) + 0.0722d * linearize(blue);
    }

    /**
     * Ratio de contraste WCAG entre cette couleur et une autre.
     *
     * @return une valeur dans [1.0, 21.0] ; 1.0 signifie deux couleurs identiques
     *         en luminance (cas du blanc sur blanc), 21.0 le contraste maximal
     */
    public double contrastRatio(RgbColor other) {
        double first = this.relativeLuminance();
        double second = other.relativeLuminance();
        double lighter = Math.max(first, second);
        double darker = Math.min(first, second);
        return (lighter + 0.05d) / (darker + 0.05d);
    }

    /** Representation hexadecimale, utilisee dans le rapport d'inspection. */
    public String toHex() {
        return String.format(Locale.ROOT, "#%02X%02X%02X", toByte(red), toByte(green), toByte(blue));
    }

    private static double linearize(float channel) {
        double value = channel;
        return value <= 0.04045d ? value / 12.92d : Math.pow((value + 0.055d) / 1.055d, 2.4d);
    }

    private static int toByte(float channel) {
        return Math.round(channel * 255f);
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 0f;
        }
        return Math.min(1f, Math.max(0f, value));
    }
}
