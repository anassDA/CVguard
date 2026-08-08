package ma.emsi.cvguard.infrastructure.ocr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ma.emsi.cvguard.domain.port.DocumentExtractionException;
import ma.emsi.cvguard.domain.port.PageRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

/**
 * Rendu graphique des pages PDF (etape [4] de la chaine de traitement, regle R6).
 *
 * <p>Le rendu est l'operation qui rend l'attaque visible : un texte blanc sur blanc,
 * transparent ou declare invisible par un mecanisme natif du format est present dans
 * le flux d'extraction mais absent de l'image produite. C'est precisement cet ecart
 * que la regle R6 mesure entre le texte natif et le texte OCR.</p>
 *
 * <p>Le rendu est limite au format PDF : le cahier des charges exclut un moteur de mise
 * en page pour le DOCX (chapitre 7.4), et {@link #canRender(byte[])} retire le
 * composant en consequence.</p>
 */
@Component
public class PdfPageRenderer implements PageRenderer {

    @Override
    public boolean canRender(byte[] content) {
        if (content == null || content.length < 5) {
            return false;
        }
        byte[] header = {'%', 'P', 'D', 'F', '-'};
        for (int i = 0; i < header.length; i++) {
            if (content[i] != header[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<BufferedImage> render(byte[] content, int dpi) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<BufferedImage> images = new ArrayList<>(document.getNumberOfPages());
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                images.add(renderer.renderImageWithDPI(i, dpi, ImageType.RGB));
            }
            return images;
        } catch (IOException unreadable) {
            throw new DocumentExtractionException(
                    "Le document PDF n'a pu etre rendu graphiquement : " + unreadable.getMessage(), unreadable);
        }
    }
}
