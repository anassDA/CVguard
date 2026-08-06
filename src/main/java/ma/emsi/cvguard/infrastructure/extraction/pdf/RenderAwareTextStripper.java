package ma.emsi.cvguard.infrastructure.extraction.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ma.emsi.cvguard.domain.model.BoundingBox;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.RgbColor;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.text.TextNormalizer;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * Extracteur de texte conservant les attributs de rendu de chaque glyphe (EF-02).
 *
 * <p>{@link PDFTextStripper} restitue le texte, pas la maniere dont il est peint. Trois
 * ajustements sont necessaires pour obtenir les attributs dont dependent les regles R1
 * a R3.</p>
 *
 * <h2>1. Les operateurs de couleur ne sont pas enregistres</h2>
 * <p>{@code PDFTextStripper} n'installe que les operateurs utiles a l'extraction de
 * texte. Les operateurs de couleur — {@code rg}, {@code g}, {@code k}, {@code sc},
 * {@code scn}, {@code cs} — n'en font pas partie : l'etat graphique conserve alors sa
 * couleur initiale, le noir, quelle que soit la couleur reellement appliquee. Un texte
 * blanc sur blanc est ainsi restitue comme du texte noir, et la regle R2 ne verrait
 * jamais rien. Ces operateurs sont donc ajoutes explicitement.</p>
 *
 * <h2>2. La taille declaree n'est pas la taille rendue</h2>
 * <p>{@link TextPosition#getFontSizeInPt()} retourne la taille declaree par l'operateur
 * {@code Tf}, sans tenir compte de la matrice de transformation courante. Un document
 * qui declare un corps 12 puis applique une echelle de 0,1 produit un texte rendu a
 * 1,2 pt tout en annoncant 12. Une regle fondee sur cette methode serait contournee par
 * une simple matrice. {@link TextPosition#getYScale()} restitue en revanche la taille
 * effectivement rendue : c'est elle qui fait foi.</p>
 *
 * <h2>3. Le repere n'est pas celui du PDF</h2>
 * <p>Les coordonnees d'une {@code TextPosition} ont pour origine le coin superieur
 * gauche de la zone d'affichage, l'axe Y descendant. Le rapport d'inspection localise
 * les detections dans le repere du PDF, origine en bas a gauche : la conversion est
 * faite ici, une fois pour toutes.</p>
 *
 * <h2>4. L'etat graphique n'est plus disponible au moment de l'ecriture</h2>
 * <p>{@code PDFTextStripper} collecte l'integralite des glyphes d'une page avant
 * d'appeler {@code writeString}. A cet instant le flux de contenu est entierement
 * parcouru et l'etat graphique a retrouve ses valeurs initiales : y consulter la
 * couleur retournerait invariablement du noir. Couleur et opacite sont donc relevees
 * dans {@code processTextPosition}, seul moment ou l'etat graphique est celui du
 * glyphe, puis associees a leur position jusqu'a la constitution des fragments.</p>
 *
 * <h2>5. Un mode de rendu peut masquer le texte sans toucher a sa couleur</h2>
 * <p>L'operateur {@code Tr} (mode de rendu de texte) peut valoir 3 : le glyphe n'est
 * ni rempli ni trace, donc invisible, quelle que soit sa couleur declaree. Ce mode
 * est utilise legitimement par la couche de texte superposee a l'image d'un PDF
 * scanne — c'est ainsi qu'un tel document reste selectionnable et cherchable — mais
 * rien n'empeche un attaquant du meme mecanisme pour dissimuler une instruction. La
 * regle R2 doit voir ce texte comme si son contraste etait nul.</p>
 */
public class RenderAwareTextStripper extends PDFTextStripper {

    /** Ecart horizontal, en fraction du corps, au-dela duquel une espace est restituee. */
    private static final float SPACE_GAP_RATIO = 0.25f;

    /**
     * Ecart horizontal, en fraction du corps, au-dela duquel deux glyphes cessent
     * d'appartenir au meme fragment. Separe notamment les colonnes d'une mise en page
     * sur deux colonnes.
     */
    private static final float FRAGMENT_GAP_RATIO = 4f;

    private final List<TextFragment> fragments = new ArrayList<>();

    /**
     * Peinture relevee pour chaque glyphe de la page courante. L'identite est la seule
     * cle valable : deux glyphes distincts peuvent etre egaux au sens de {@code equals}.
     */
    private final Map<TextPosition, Paint> paints = new IdentityHashMap<>();

    private Accumulator accumulator;
    private int nextIndex;

    public RenderAwareTextStripper() throws IOException {
        setSortByPosition(true);
        registerColourOperators();
    }

    /**
     * Parcourt le document et restitue ses fragments.
     *
     * <p>Le texte produit par la classe mere est ignore : seul le flux de glyphes
     * intercepte nous interesse.</p>
     */
    public List<TextFragment> extract(PDDocument document) throws IOException {
        fragments.clear();
        paints.clear();
        accumulator = null;
        nextIndex = 0;

        getText(document);
        flush();

        return List.copyOf(fragments);
    }

    private void registerColourOperators() {
        addOperator(new SetNonStrokingColorSpace(this));
        addOperator(new SetNonStrokingDeviceRGBColor(this));
        addOperator(new SetNonStrokingDeviceGrayColor(this));
        addOperator(new SetNonStrokingDeviceCMYKColor(this));
        addOperator(new SetNonStrokingColor(this));
        addOperator(new SetNonStrokingColorN(this));
    }

    @Override
    protected void startPage(PDPage page) throws IOException {
        flush();
        // La peinture relevee ne sert qu'a la page en cours : la liberer evite de
        // conserver en memoire un releve par glyphe pour l'ensemble du document.
        paints.clear();
        super.startPage(page);
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        flush();
        super.endPage(page);
    }

    /**
     * Seul point du traitement ou l'etat graphique est celui du glyphe.
     *
     * <p>Couleur et opacite y sont relevees et conservees jusqu'a la constitution des
     * fragments, qui intervient une fois la page entierement parcourue.</p>
     */
    @Override
    protected void processTextPosition(TextPosition glyph) {
        RenderingMode mode = getGraphicsState().getTextState().getRenderingMode();
        paints.put(glyph, new Paint(
                nonStrokingColour(),
                round(getGraphicsState().getNonStrokeAlphaConstant()),
                !mode.isFill() && !mode.isStroke()));
        super.processTextPosition(glyph);
    }

    /**
     * Point d'interception : la classe mere appelle cette methode ligne par ligne.
     *
     * <p>La chaine reconstituee par PDFBox est ecartee au profit des positions, seules
     * porteuses des attributs de rendu. Un fragment est clos des qu'un attribut change
     * ou qu'un ecart horizontal important separe deux glyphes.</p>
     */
    @Override
    protected void writeString(String reconstructedText, List<TextPosition> positions) {
        for (TextPosition glyph : positions) {
            accept(glyph);
        }
        // Chaque appel correspond a une ligne : un fragment ne chevauche pas deux lignes.
        flush();
    }

    private void accept(TextPosition glyph) {
        String unicode = glyph.getUnicode();
        if (unicode == null || unicode.isEmpty()) {
            return;
        }

        PDRectangle cropBox = getCurrentPage().getCropBox();
        float fontSizePt = round(glyph.getYScale());
        float left = cropBox.getLowerLeftX() + glyph.getXDirAdj();
        float width = glyph.getWidthDirAdj();
        float height = glyph.getHeightDir();
        // getYDirAdj() donne la ligne de base mesuree depuis le haut de la zone visible.
        float baseline = cropBox.getUpperRightY() - glyph.getYDirAdj();

        Paint paint = paints.getOrDefault(glyph, Paint.OPAQUE_BLACK);
        Attributes attributes = new Attributes(
                getCurrentPageNo(),
                glyph.getFont() == null ? null : glyph.getFont().getName(),
                fontSizePt,
                paint.colour(),
                paint.alpha(),
                paint.hidden());

        if (accumulator == null || !accumulator.attributes.equals(attributes)) {
            flush();
            accumulator = new Accumulator(attributes);
        } else {
            float gap = left - accumulator.right;
            float reference = Math.max(fontSizePt, 1f);
            if (gap > FRAGMENT_GAP_RATIO * reference) {
                flush();
                accumulator = new Accumulator(attributes);
            } else if (gap > SPACE_GAP_RATIO * reference) {
                accumulator.appendSpace();
            }
        }

        accumulator.append(unicode, left, baseline, width, height);
    }

    private void flush() {
        if (accumulator == null || accumulator.isEmpty()) {
            accumulator = null;
            return;
        }
        fragments.add(accumulator.toFragment(nextIndex++));
        accumulator = null;
    }

    /**
     * Couleur de remplissage courante.
     *
     * <p>Certains espaces colorimetriques — Separation, DeviceN adosses a un profil
     * exotique — ne sont pas convertibles en RVB. On retient alors le noir : c'est la
     * valeur qui ne declenche pas R2, et il vaut mieux manquer une detection que
     * signaler un document legitime sur une couleur qu'on n'a pas su lire.</p>
     */
    private RgbColor nonStrokingColour() {
        try {
            return RgbColor.ofPacked(getGraphicsState().getNonStrokingColor().toRGB());
        } catch (IOException | RuntimeException unconvertibleColourSpace) {
            return RgbColor.BLACK;
        }
    }

    private static float round(double value) {
        return Math.round(value * 100d) / 100f;
    }

    /** Couleur, opacite et visibilite relevees au moment ou le glyphe est peint. */
    private record Paint(RgbColor colour, float alpha, boolean hidden) {

        private static final Paint OPAQUE_BLACK = new Paint(RgbColor.BLACK, 1f, false);
    }

    /** Attributs de rendu dont l'egalite definit l'homogeneite d'un fragment. */
    private record Attributes(
            int page, String fontName, float fontSizePt, RgbColor colour, float alpha, boolean hidden) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Attributes that
                    && page == that.page
                    && Objects.equals(fontName, that.fontName)
                    && Float.compare(fontSizePt, that.fontSizePt) == 0
                    && Objects.equals(colour, that.colour)
                    && Float.compare(alpha, that.alpha) == 0
                    && hidden == that.hidden;
        }

        @Override
        public int hashCode() {
            return Objects.hash(page, fontName, fontSizePt, colour, alpha, hidden);
        }
    }

    /** Fragment en cours de constitution. */
    private static final class Accumulator {

        private final Attributes attributes;
        private final StringBuilder text = new StringBuilder();

        private float left = Float.MAX_VALUE;
        private float bottom = Float.MAX_VALUE;
        private float right = -Float.MAX_VALUE;
        private float top = -Float.MAX_VALUE;

        private Accumulator(Attributes attributes) {
            this.attributes = attributes;
        }

        private void append(String unicode, float glyphLeft, float baseline, float width, float height) {
            text.append(unicode);
            left = Math.min(left, glyphLeft);
            right = Math.max(right, glyphLeft + width);
            bottom = Math.min(bottom, baseline);
            top = Math.max(top, baseline + height);
        }

        private void appendSpace() {
            if (text.length() > 0 && text.charAt(text.length() - 1) != ' ') {
                text.append(' ');
            }
        }

        private boolean isEmpty() {
            return text.isEmpty() || text.toString().isBlank();
        }

        private TextFragment toFragment(int index) {
            return new TextFragment(
                    index,
                    attributes.page(),
                    TextNormalizer.nfkc(text.toString()),
                    attributes.fontName(),
                    attributes.fontSizePt(),
                    attributes.colour(),
                    // Le fond n'est pas connu a ce stade : le blanc est le fond par defaut
                    // d'une page PDF non peinte. Le recouvrement par un element graphique
                    // opaque (aplat, image) est traite par la regle R3 elle-meme (J4), qui
                    // analyse l'ordre de peinture du flux de contenu.
                    RgbColor.WHITE,
                    attributes.alpha(),
                    attributes.hidden(),
                    new BoundingBox(left, bottom, right - left, top - bottom),
                    FragmentOrigin.BODY);
        }
    }
}
