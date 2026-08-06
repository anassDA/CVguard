package ma.emsi.cvguard.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;

/**
 * Fabrique de documents DOCX de test.
 *
 * <p>Comme pour {@link TestPdfBuilder}, le corpus ne peut reposer sur des documents
 * reels (RGPD, loi 09-08). Chaque document est construit par programme avec des
 * attributs connus exactement.</p>
 */
public final class TestDocxBuilder {

    private final XWPFDocument document = new XWPFDocument();

    public static TestDocxBuilder create() {
        return new TestDocxBuilder();
    }

    /** Paragraphe en un seul run, corps 11 noir, le cas nominal d'un CV. */
    public TestDocxBuilder paragraph(String text) {
        return paragraph(text, 11, "000000");
    }

    public TestDocxBuilder paragraph(String text, int fontSizePt, String hexColor) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(fontSizePt);
        run.setColor(hexColor);
        return this;
    }

    /**
     * Paragraphe dont le run porte l'attribut de texte masque natif de Word
     * ({@code w:vanish}, Format > Police > Masque).
     */
    public TestDocxBuilder hiddenParagraph(String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(11);
        run.setColor("000000");

        CTRPr properties = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        properties.addNewVanish();
        return this;
    }

    /** Paragraphe sans taille ni couleur explicites, pour verifier le repli documente. */
    public TestDocxBuilder paragraphWithoutExplicitStyle(String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        return this;
    }

    /**
     * Tableau a deux colonnes, mise en page frequente d'un CV. Chaque cellule recoit
     * un unique paragraphe.
     */
    public TestDocxBuilder twoColumnTable(String leftText, String rightText) {
        XWPFTable table = document.createTable(1, 2);
        setCellText(table, 0, 0, leftText);
        setCellText(table, 0, 1, rightText);
        return this;
    }

    private void setCellText(XWPFTable table, int row, int col, String text) {
        var cell = table.getRow(row).getCell(col);
        // getTableCells() cree deja un paragraphe vide par defaut : on le reutilise.
        XWPFParagraph paragraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(11);
        run.setColor("000000");
    }

    public TestDocxBuilder header(String text) {
        XWPFHeader header = document.createHeader(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
        XWPFParagraph paragraph = header.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        return this;
    }

    public TestDocxBuilder footer(String text) {
        XWPFFooter footer = document.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
        XWPFParagraph paragraph = footer.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        return this;
    }

    public TestDocxBuilder comment(String text) {
        var comments = document.getDocComments() != null ? document.getDocComments() : document.createComments();
        var comment = comments.createComment(java.math.BigInteger.valueOf(comments.getCtComments().sizeOfCommentArray()));
        XWPFParagraph paragraph = comment.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        return this;
    }

    public TestDocxBuilder coreProperty(String name, String value) {
        var core = document.getProperties().getCoreProperties();
        switch (name) {
            case "Title" -> core.setTitle(value);
            case "Subject" -> core.setSubjectProperty(value);
            case "Keywords" -> core.setKeywords(value);
            case "Description" -> core.setDescription(value);
            default -> throw new IllegalArgumentException("Propriete non geree par le banc de test : " + name);
        }
        return this;
    }

    public byte[] build() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.write(output);
            return output.toByteArray();
        } catch (IOException failure) {
            throw new UncheckedIOException("Construction du DOCX de test impossible", failure);
        }
    }
}
