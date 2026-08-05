package ma.emsi.cvguard.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

/**
 * Fabrique de documents PDF de test.
 *
 * <p>Le corpus de validation ne peut reposer sur des CV reels (contrainte RGPD et loi
 * 09-08). Les documents sont donc construits par programme, ce qui presente un second
 * avantage : chaque attribut de rendu — corps, couleur, position — est connu
 * exactement, et les assertions portent sur des valeurs verifiables plutot que sur le
 * resultat d'une inspection visuelle.</p>
 *
 * <p>Cette fabrique sert aux tests d'extraction et sera reprise pour la generation du
 * corpus de validation.</p>
 */
public final class TestPdfBuilder {

    private final List<PageSpec> pages = new ArrayList<>();
    private final Map<String, String> metadata = new LinkedHashMap<>();
    private PageSpec current;

    private TestPdfBuilder(PDRectangle size) {
        newPage(size);
    }

    /** Document A4 portrait, format usuel d'un CV. */
    public static TestPdfBuilder a4() {
        return new TestPdfBuilder(PDRectangle.A4);
    }

    public static TestPdfBuilder ofSize(PDRectangle size) {
        return new TestPdfBuilder(size);
    }

    public TestPdfBuilder newPage() {
        return newPage(PDRectangle.A4);
    }

    public TestPdfBuilder newPage(PDRectangle size) {
        current = new PageSpec(size);
        pages.add(current);
        return this;
    }

    /** Texte noir en corps 11, le cas nominal d'un CV. */
    public TestPdfBuilder text(String content, float x, float y) {
        return text(content, x, y, 11f, 0f, 0f, 0f);
    }

    public TestPdfBuilder text(String content, float x, float y, float fontSizePt) {
        return text(content, x, y, fontSizePt, 0f, 0f, 0f);
    }

    /**
     * Texte dont tous les attributs de rendu sont imposes.
     *
     * @param content contenu
     * @param x       abscisse de la ligne de base, en points, origine en bas a gauche
     * @param y       ordonnee de la ligne de base
     * @param fontSizePt corps typographique
     * @param red,green,blue composantes de la couleur de remplissage, dans [0, 1]
     */
    public TestPdfBuilder text(
            String content, float x, float y, float fontSizePt, float red, float green, float blue) {
        current.operations.add(stream -> {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSizePt);
            stream.setNonStrokingColor(red, green, blue);
            stream.newLineAtOffset(x, y);
            stream.showText(content);
            stream.endText();
        });
        return this;
    }

    /**
     * Texte dont le corps declare est reduit par une matrice de transformation.
     *
     * <p>Vecteur de dissimulation plus fin que le corps minuscule declare : la police
     * annonce une taille ordinaire, et c'est l'echelle appliquee qui rend le texte
     * illisible. Seule la taille effective, matrice appliquee, revele l'attaque.</p>
     *
     * @param declaredFontSizePt corps declare par l'operateur Tf
     * @param scale              facteur d'echelle applique par la matrice courante
     */
    public TestPdfBuilder scaledText(
            String content, float x, float y, float declaredFontSizePt, float scale) {
        current.operations.add(stream -> {
            stream.saveGraphicsState();
            stream.transform(Matrix.getScaleInstance(scale, scale));
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), declaredFontSizePt);
            stream.setNonStrokingColor(0f, 0f, 0f);
            stream.newLineAtOffset(x / scale, y / scale);
            stream.showText(content);
            stream.endText();
            stream.restoreGraphicsState();
        });
        return this;
    }

    /**
     * Texte peint avec une opacite reduite.
     *
     * <p>Troisieme voie de dissimulation : la couleur reste noire et le corps ordinaire,
     * seule l'opacite rend le texte invisible.</p>
     */
    public TestPdfBuilder transparentText(
            String content, float x, float y, float fontSizePt, float alpha) {
        current.operations.add(stream -> {
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant(alpha);

            stream.saveGraphicsState();
            stream.setGraphicsStateParameters(state);
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSizePt);
            stream.setNonStrokingColor(0f, 0f, 0f);
            stream.newLineAtOffset(x, y);
            stream.showText(content);
            stream.endText();
            stream.restoreGraphicsState();
        });
        return this;
    }

    /** Aplat de couleur, utilise pour masquer du texte ou lui donner un fond. */
    public TestPdfBuilder filledRectangle(
            float x, float y, float width, float height, float red, float green, float blue) {
        current.operations.add(stream -> {
            stream.setNonStrokingColor(red, green, blue);
            stream.addRect(x, y, width, height);
            stream.fill();
        });
        return this;
    }

    public TestPdfBuilder metadata(String key, String value) {
        metadata.put(key, value);
        return this;
    }

    public byte[] build() {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            for (PageSpec spec : pages) {
                PDPage page = new PDPage(spec.size);
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    for (ContentOperation operation : spec.operations) {
                        operation.applyTo(stream);
                    }
                }
            }

            applyMetadata(document);
            document.save(output);
            return output.toByteArray();
        } catch (IOException failure) {
            throw new UncheckedIOException("Construction du PDF de test impossible", failure);
        }
    }

    private void applyMetadata(PDDocument document) {
        var information = document.getDocumentInformation();
        metadata.forEach((key, value) -> {
            switch (key) {
                case "Title" -> information.setTitle(value);
                case "Subject" -> information.setSubject(value);
                case "Keywords" -> information.setKeywords(value);
                case "Author" -> information.setAuthor(value);
                case "Creator" -> information.setCreator(value);
                case "Producer" -> information.setProducer(value);
                default -> information.setCustomMetadataValue(key, value);
            }
        });
    }

    private static final class PageSpec {

        private final PDRectangle size;
        private final List<ContentOperation> operations = new ArrayList<>();

        private PageSpec(PDRectangle size) {
            this.size = size;
        }
    }

    @FunctionalInterface
    private interface ContentOperation {

        void applyTo(PDPageContentStream stream) throws IOException;
    }
}
