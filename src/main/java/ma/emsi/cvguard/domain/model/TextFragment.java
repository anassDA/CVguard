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
 * @param hidden     invisibilite declaree par un mecanisme natif du format, independant
 *                   de la couleur, de la taille ou de la position : attribut de texte
 *                   masque en DOCX ({@code w:vanish}), mode de rendu de texte invisible
 *                   en PDF ({@code Tr 3}, utilise aussi legitimement par les couches OCR
 *                   d'un PDF scanne)
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
        boolean hidden,
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
        return new TextFragment(index, 0, text, null, 0f, null, null, 1f, false, null, origin);
    }

    public int length() {
        return text.length();
    }

    public boolean isBlank() {
        return text.isBlank();
    }

    /**
     * Indique si le fragment porte des attributs typographiques exploitables — police,
     * corps, couleur — independamment de sa position.
     *
     * <p>Un DOCX n'offre aucune information de position sans moteur de mise en page,
     * que le cahier des charges n'impose pas (chapitre 7.4 : POI donne acces aux
     * proprietes de style, pas au rendu). Fonder cette methode sur la seule presence
     * d'une boite aurait pour effet d'exclure tous les fragments DOCX du calcul du
     * corps median et des regles R1 et R2, qui n'ont pourtant besoin d'aucune
     * coordonnee.</p>
     */
    public boolean isRendered() {
        return fontSizePt > 0f;
    }

    /** Indique si la position du fragment sur la page est connue, prealable a la regle R3. */
    public boolean hasKnownPosition() {
        return box != null && page > 0;
    }

    /**
     * Couleur reellement percue : la couleur du glyphe composee sur son fond selon son
     * opacite, ou le fond lui-meme si le fragment est declare invisible par un attribut
     * natif du format.
     *
     * <p>Un texte noir d'opacite nulle, un texte au mode de rendu invisible et un texte
     * blanc sur blanc produisent le meme effet a l'ecran : aucun. Les composer avant de
     * mesurer le contraste evite de traiter separement trois variantes du meme
     * vecteur de dissimulation.</p>
     */
    public RgbColor effectiveColor() {
        if (hidden) {
            return background;
        }
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
                index, page, newText, fontName, fontSizePt, color, background, alpha, hidden, box, origin);
    }
}
