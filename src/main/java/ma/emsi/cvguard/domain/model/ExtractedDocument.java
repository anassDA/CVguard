package ma.emsi.cvguard.domain.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resultat de l'etape [1] EXTRACTION de la chaine de traitement : l'ensemble du
 * contenu du document, accompagne de ses attributs de rendu.
 *
 * <p>Cette structure est la seule entree du moteur de regles. Elle est volontairement
 * independante de la bibliotheque d'extraction utilisee (PDFBox ou POI), ce qui permet
 * de tester les regles sans document reel.</p>
 *
 * @param documentId identifiant de correlation de l'inspection (EF-14)
 * @param format     format reel du document
 * @param filename   nom de fichier declare par l'appelant, a des fins de journalisation
 * @param pages      geometrie de chaque page ; vide pour un DOCX, dont la pagination
 *                   n'est pas connue avant le rendu
 * @param fragments  fragments de texte dans l'ordre du flux d'extraction
 * @param metadata   champs de metadonnees du document (Title, Subject, Keywords...)
 */
public record ExtractedDocument(
        String documentId,
        DocumentFormat format,
        String filename,
        List<PageGeometry> pages,
        List<TextFragment> fragments,
        Map<String, String> metadata) {

    public ExtractedDocument {
        pages = pages == null ? List.of() : List.copyOf(pages);
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** Nombre de pages, ou 0 lorsque la pagination n'est pas connue. */
    public int pageCount() {
        return pages.size();
    }

    /** Fragments effectivement rendus sur une page, seuls concernes par R1 a R3. */
    public List<TextFragment> renderedFragments() {
        return fragments.stream().filter(TextFragment::isRendered).toList();
    }

    /**
     * Texte natif du document : la concatenation des fragments du corps, dans
     * l'ordre d'extraction. C'est ce texte qu'un parseur naif transmettrait au
     * modele, et celui que la regle R6 compare au rendu OCR.
     */
    public String nativeText() {
        return fragments.stream()
                .filter(fragment -> fragment.origin() != FragmentOrigin.METADATA)
                .map(TextFragment::text)
                .collect(Collectors.joining());
    }

    /** Geometrie d'une page donnee, ou {@code null} si la page est inconnue. */
    public PageGeometry page(int number) {
        return pages.stream()
                .filter(page -> page.number() == number)
                .findFirst()
                .orElse(null);
    }
}
