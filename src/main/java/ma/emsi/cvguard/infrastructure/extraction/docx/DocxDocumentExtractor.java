package ma.emsi.cvguard.infrastructure.extraction.docx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.RgbColor;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.port.DocumentExtractionException;
import ma.emsi.cvguard.domain.port.DocumentExtractor;
import ma.emsi.cvguard.domain.text.TextNormalizer;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFComment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Extraction d'un document DOCX (etape [1] de la chaine de traitement).
 *
 * <p>Le cahier des charges limite volontairement le perimetre DOCX a l'acces aux
 * proprietes de style au niveau du XML du document (chapitre 7.4) : contrairement au
 * PDF, aucun moteur de mise en page n'entre dans la pile technique retenue. Trois
 * consequences en decoulent, documentees ici pour ne pas laisser croire a une parite
 * complete avec l'extraction PDF :</p>
 *
 * <ul>
 *   <li><b>Aucune position.</b> {@link ExtractedDocument#pages()} reste vide et chaque
 *       fragment a une boite englobante nulle : la regle R3 (positionnement hors zone)
 *       ne s'applique pas a un DOCX dans cette version.</li>
 *   <li><b>Aucun fond.</b> La trame de fond eventuelle d'un paragraphe ({@code w:shd})
 *       n'est pas relevee ; le fond retenu par {@link TextFragment} reste le blanc par
 *       defaut. Une injection dissimulee par correspondance de trame plutot que de
 *       couleur de police echapperait donc a la regle R2.</li>
 *   <li><b>Corps par defaut approche.</b> Lorsqu'un run ne declare pas de corps
 *       explicite, il herite en principe d'une cascade de styles (style du run, style
 *       du paragraphe, styles par defaut du document). Reconstruire cette cascade est
 *       hors de portee du present projet ; un corps conventionnel de 11 pt — le defaut
 *       usuel de Word et LibreOffice — est retenu a la place.</li>
 * </ul>
 *
 * <p>Le texte cache par l'attribut natif {@code w:vanish} (Format > Police > Masque)
 * est en revanche pleinement pris en charge : c'est le moyen le plus direct de
 * dissimuler une instruction dans un DOCX, accessible depuis l'interface de Word sans
 * manipulation de couleur ni de corps.</p>
 */
@Component
public class DocxDocumentExtractor implements DocumentExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocxDocumentExtractor.class);

    /** Corps retenu quand un run ne declare pas de taille explicite (chapitre ci-dessus). */
    private static final float DEFAULT_FONT_SIZE_PT = 11f;

    @Override
    public DocumentFormat format() {
        return DocumentFormat.DOCX;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String documentId, String filename) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<TextFragment> fragments = new ArrayList<>();

            for (XWPFHeader header : document.getHeaderList()) {
                appendParagraphs(fragments, header.getParagraphs(), FragmentOrigin.HEADER);
            }

            appendBodyElements(fragments, document.getBodyElements());

            for (XWPFFooter footer : document.getFooterList()) {
                appendParagraphs(fragments, footer.getParagraphs(), FragmentOrigin.FOOTER);
            }

            // getComments() suppose la presence d'une partie commentaires : l'appeler
            // sur un document qui n'en declare aucune leve une NullPointerException
            // interne a POI plutot que de retourner un tableau vide.
            if (document.getDocComments() != null) {
                for (XWPFComment comment : document.getComments()) {
                    appendNonRendered(fragments, comment.getText(), FragmentOrigin.COMMENT);
                }
            }

            Map<String, String> metadata = readMetadata(document);
            for (String value : metadata.values()) {
                appendNonRendered(fragments, value, FragmentOrigin.METADATA);
            }

            log.debug("Document {} extrait : {} fragment(s), {} metadonnee(s)",
                    documentId, fragments.size(), metadata.size());

            return new ExtractedDocument(documentId, DocumentFormat.DOCX, filename, List.of(), fragments, metadata);

        } catch (IOException | RuntimeException unreadable) {
            throw new DocumentExtractionException(
                    "Le document DOCX n'a pu etre parcouru : " + unreadable.getMessage(), unreadable);
        }
    }

    /**
     * Parcourt le corps du document dans son ordre naturel : paragraphes et tableaux
     * s'y succedent tels qu'ils apparaissent a l'ecran, une mise en page a deux
     * colonnes reposant frequemment sur un tableau.
     */
    private void appendBodyElements(List<TextFragment> fragments, List<IBodyElement> elements) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph) {
                appendParagraph(fragments, paragraph, FragmentOrigin.BODY);
            } else if (element instanceof XWPFTable table) {
                appendTable(fragments, table);
            }
        }
    }

    private void appendTable(List<TextFragment> fragments, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                appendParagraphs(fragments, cell.getParagraphs(), FragmentOrigin.BODY);
            }
        }
    }

    private void appendParagraphs(
            List<TextFragment> fragments, List<XWPFParagraph> paragraphs, FragmentOrigin origin) {
        for (XWPFParagraph paragraph : paragraphs) {
            appendParagraph(fragments, paragraph, origin);
        }
    }

    private void appendParagraph(List<TextFragment> fragments, XWPFParagraph paragraph, FragmentOrigin origin) {
        // Un run est deja, par definition OOXML, une plage maximale de mise en forme
        // homogene : contrairement au PDF, aucun regroupement supplementaire n'est
        // necessaire pour obtenir des fragments coherents.
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text == null || text.isBlank()) {
                continue;
            }
            fragments.add(new TextFragment(
                    fragments.size(),
                    0,
                    TextNormalizer.nfkc(text),
                    run.getFontFamily(),
                    fontSizeOf(run),
                    colourOf(run),
                    RgbColor.WHITE,
                    1f,
                    isHidden(run),
                    null,
                    origin));
        }
    }

    private void appendNonRendered(List<TextFragment> fragments, String text, FragmentOrigin origin) {
        if (text == null || text.isBlank()) {
            return;
        }
        fragments.add(TextFragment.ofNonRendered(fragments.size(), TextNormalizer.nfkc(text), origin));
    }

    private static float fontSizeOf(XWPFRun run) {
        Double declared = run.getFontSizeAsDouble();
        return declared == null ? DEFAULT_FONT_SIZE_PT : declared.floatValue();
    }

    /**
     * Couleur du run.
     *
     * <p>{@link XWPFRun#getColor()} retourne {@code null} lorsque la couleur est
     * heritee du theme, et la chaine {@code "auto"} lorsqu'elle est explicitement
     * laissee automatique : les deux cas retombent sur le noir, couleur par defaut
     * du texte courant.</p>
     */
    private static RgbColor colourOf(XWPFRun run) {
        String hex = run.getColor();
        if (hex == null || "auto".equalsIgnoreCase(hex)) {
            return RgbColor.BLACK;
        }
        try {
            return RgbColor.ofPacked(Integer.parseInt(hex, 16));
        } catch (NumberFormatException malformedColour) {
            return RgbColor.BLACK;
        }
    }

    /**
     * Indique si le run porte l'attribut {@code w:vanish}.
     *
     * <p>Present sans attribut {@code w:val}, l'element signifie "vrai" au sens
     * d'OOXML ; seule une valeur explicitement fausse ({@code 0}, {@code false},
     * {@code off}) neutralise l'attribut.</p>
     */
    private static boolean isHidden(XWPFRun run) {
        if (!run.getCTR().isSetRPr()) {
            return false;
        }
        var vanish = run.getCTR().getRPr().getVanishList();
        if (vanish.isEmpty()) {
            return false;
        }
        Object value = vanish.get(0).isSetVal() ? vanish.get(0).getVal() : null;
        if (value == null) {
            return true;
        }
        String text = value.toString();
        return !"false".equalsIgnoreCase(text) && !"0".equals(text) && !"off".equalsIgnoreCase(text);
    }

    private static Map<String, String> readMetadata(XWPFDocument document) {
        Map<String, String> metadata = new LinkedHashMap<>();
        POIXMLProperties.CoreProperties core = document.getProperties().getCoreProperties();
        put(metadata, "Title", core.getTitle());
        put(metadata, "Subject", core.getSubject());
        put(metadata, "Keywords", core.getKeywords());
        put(metadata, "Description", core.getDescription());
        return metadata;
    }

    private static void put(Map<String, String> metadata, String key, String value) {
        String normalized = TextNormalizer.nfkc(value);
        if (normalized != null && !normalized.isBlank()) {
            metadata.put(key, normalized);
        }
    }
}
