package ma.emsi.cvguard.infrastructure.extraction.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.model.BoundingBox;
import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.PageGeometry;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.port.DocumentExtractionException;
import ma.emsi.cvguard.domain.port.DocumentExtractor;
import ma.emsi.cvguard.domain.text.TextNormalizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Extraction d'un document PDF (etape [1] de la chaine de traitement).
 *
 * <p>Trois sources sont parcourues, car une injection peut se loger dans chacune :
 * le corps du document, les champs de metadonnees et les annotations. Les deux
 * dernieres sont rarement inspectees et constituent a ce titre un vecteur commode.</p>
 *
 * <p>Le document est charge et traite integralement en memoire, puis libere : aucun
 * fichier temporaire n'est ecrit sur disque (ENF-05).</p>
 */
@Component
public class PdfDocumentExtractor implements DocumentExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentExtractor.class);

    @Override
    public DocumentFormat format() {
        return DocumentFormat.PDF;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String documentId, String filename) {
        try (PDDocument document = Loader.loadPDF(content)) {
            List<PageGeometry> pages = readGeometry(document);
            List<TextFragment> fragments = new ArrayList<>(new RenderAwareTextStripper().extract(document));

            Map<String, String> metadata = readMetadata(document);
            appendNonRendered(fragments, metadata.values(), FragmentOrigin.METADATA);
            appendNonRendered(fragments, readAnnotations(document), FragmentOrigin.COMMENT);

            log.debug("Document {} extrait : {} page(s), {} fragment(s), {} metadonnee(s)",
                    documentId, pages.size(), fragments.size(), metadata.size());

            return new ExtractedDocument(documentId, DocumentFormat.PDF, filename, pages, fragments, metadata);

        } catch (InvalidPasswordException protectedDocument) {
            throw new DocumentExtractionException(
                    "Le document est protege par mot de passe : son contenu ne peut etre inspecte.",
                    protectedDocument);
        } catch (IOException | RuntimeException unreadable) {
            throw new DocumentExtractionException(
                    "Le document PDF n'a pu etre parcouru : " + unreadable.getMessage(), unreadable);
        }
    }

    private static List<PageGeometry> readGeometry(PDDocument document) {
        List<PageGeometry> pages = new ArrayList<>(document.getNumberOfPages());
        int number = 1;
        for (PDPage page : document.getPages()) {
            pages.add(new PageGeometry(
                    number++,
                    toBoundingBox(page.getMediaBox()),
                    toBoundingBox(page.getCropBox()),
                    page.getRotation()));
        }
        return pages;
    }

    /**
     * Champs de metadonnees non vides, standard et personnalises.
     *
     * <p>Les champs personnalises sont inclus : rien n'empeche un attaquant d'inscrire
     * sa charge dans une cle de son choix plutot que dans {@code Title}.</p>
     */
    private static Map<String, String> readMetadata(PDDocument document) {
        Map<String, String> metadata = new LinkedHashMap<>();
        PDDocumentInformation information = document.getDocumentInformation();
        if (information == null) {
            return metadata;
        }
        for (String key : information.getMetadataKeys()) {
            String value = TextNormalizer.nfkc(information.getCustomMetadataValue(key));
            if (value != null && !value.isBlank()) {
                metadata.put(key, value);
            }
        }
        return metadata;
    }

    /** Contenu textuel des annotations : notes, commentaires de revision, champs de formulaire. */
    private static List<String> readAnnotations(PDDocument document) {
        List<String> contents = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            try {
                for (PDAnnotation annotation : page.getAnnotations()) {
                    String value = annotation.getContents();
                    if (value != null && !value.isBlank()) {
                        contents.add(value);
                    }
                }
            } catch (IOException unreadableAnnotations) {
                // Une page dont les annotations sont illisibles ne doit pas condamner
                // l'inspection du reste du document.
                log.debug("Annotations illisibles sur une page : {}", unreadableAnnotations.getMessage());
            }
        }
        return contents;
    }

    /**
     * Ajoute des fragments non rendus.
     *
     * <p>NFKC etant idempotente, normaliser ici une valeur de metadonnee deja
     * normalisee par {@code readMetadata()} est sans effet : cette methode peut donc
     * s'appliquer uniformement aux deux sources sans que l'appelant ait a se souvenir
     * de laquelle a deja ete traitee.</p>
     */
    private static void appendNonRendered(
            List<TextFragment> fragments, Iterable<String> values, FragmentOrigin origin) {
        for (String value : values) {
            fragments.add(TextFragment.ofNonRendered(fragments.size(), TextNormalizer.nfkc(value), origin));
        }
    }

    private static BoundingBox toBoundingBox(PDRectangle rectangle) {
        if (rectangle == null) {
            return null;
        }
        return new BoundingBox(
                rectangle.getLowerLeftX(),
                rectangle.getLowerLeftY(),
                rectangle.getWidth(),
                rectangle.getHeight());
    }
}
