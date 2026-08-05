package ma.emsi.cvguard.domain.model;

import java.util.Objects;

/**
 * Unite elementaire produite par l'extraction (EF-02) : un morceau de texte
 * homogene accompagne de ses attributs de rendu.
 *
 * <p>L'homogeneite porte sur la police, le corps et la couleur : des que l'un
 * de ces attributs change, l'extracteur ouvre un nouveau fragment. C'est ce qui
 * permet aux regles R1 a R3 de raisonner sur des blocs coherents plutot que
 * caractere par caractere.</p>
 *
 * @param index      rang d'apparition dans le flux d'extraction, utilise pour restituer l'ordre
 * @param page       numero de page (1-based) ; 0 lorsque le fragment n'est pas rendu sur une page
 * @param text       contenu textuel brut, avant normalisation
 * @param fontName   nom de la police telle que declaree dans le document
 * @param fontSizePt corps effectif en points, matrice de transformation appliquee
 * @param color      couleur de rendu du glyphe
 * @param background couleur du fond sous-jacent, blanc par defaut si indeterminable
 * @param alpha      opacite du remplissage, dans [0, 1] ; une opacite nulle rend le
 *                   glyphe invisible sans que sa couleur ait ete modifiee
 * @param box        boite englobante dans le repere de la page
 * @param origin     provenance du fragment
 */
public record TextFragment(
        int index,
        int page,
        String text,
        String fontName,
        float fontSizePt,
        RgbColor color,
        RgbColor background,
        float alpha,
        BoundingBox box,
        FragmentOrigin origin) {

    public TextFragment {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(origin, "origin");
        color = color == null ? RgbColor.BLACK : color;
        background = background == null ? RgbColor.WHITE : background;
        alpha = Math.min(1f, Math.max(0f, Float.isNaN(alpha) ? 1f : alpha));
    }

    /**
     * Fragment sans attributs de rendu, utilise pour les metadonnees et les commentaires
     * qui n'ont ni police ni position.
     */
    public static TextFragment ofNonRendered(int index, String text, FragmentOrigin origin) {
        return new TextFragment(index, 0, text, null, 0f, null, null, 1f, null, origin);
    }

    public int length() {
        return text.length();
    }

    public boolean isBlank() {
        return text.isBlank();
    }

    /** Indique si le fragment porte des attributs de rendu exploitables par R1 a R3. */
    public boolean isRendered() {
        return box != null && fontSizePt > 0f;
    }

    /**
     * Couleur reellement percue : la couleur du glyphe composee sur son fond selon son
     * opacite.
     *
     * <p>Un texte noir d'opacite nulle est aussi invisible qu'un texte blanc sur blanc.
     * Composer avant de mesurer le contraste evite d'avoir a traiter separement ce
     * second vecteur de dissimulation.</p>
     */
    public RgbColor effectiveColor() {
        if (alpha >= 1f) {
            return color;
        }
        return new RgbColor(
                alpha * color.red() + (1f - alpha) * background.red(),
                alpha * color.green() + (1f - alpha) * background.green(),
                alpha * color.blue() + (1f - alpha) * background.blue());
    }

    /** Ratio de contraste entre le glyphe effectivement rendu et son fond (regle R2). */
    public double contrastRatio() {
        return effectiveColor().contrastRatio(background);
    }

    /** Copie du fragment avec un texte substitue, les attributs de rendu etant conserves. */
    public TextFragment withText(String newText) {
        return new TextFragment(
                index, page, newText, fontName, fontSizePt, color, background, alpha, box, origin);
    }
}
