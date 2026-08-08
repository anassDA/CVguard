package ma.emsi.cvguard.infrastructure.extraction;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.port.DocumentExtractionException;
import ma.emsi.cvguard.domain.port.DocumentExtractor;
import org.springframework.stereotype.Component;

/**
 * Point d'entree unique de l'extraction (livrable J3 : "service d'extraction unifie").
 *
 * <p>Dispatche vers l'extracteur du format concerne, determine a partir du nom de
 * fichier declare (EF-01). Prendre en charge un nouveau format se resume a publier
 * une nouvelle implementation de {@link DocumentExtractor} : cette classe n'a pas a
 * etre modifiee, dans le meme esprit d'extensibilite que le moteur de regles
 * (ENF-07).</p>
 */
@Component
public class UnifiedDocumentExtractor {

    private final Map<DocumentFormat, DocumentExtractor> extractorsByFormat;

    public UnifiedDocumentExtractor(List<DocumentExtractor> extractors) {
        this.extractorsByFormat = new EnumMap<>(DocumentFormat.class);
        for (DocumentExtractor extractor : extractors) {
            extractorsByFormat.put(extractor.format(), extractor);
        }
    }

    /**
     * Extrait un document en fonction du format devine par le nom de fichier declare.
     *
     * @throws DocumentExtractionException si le format n'est pas reconnu ou si aucun
     *                                      extracteur n'est enregistre pour ce format
     */
    public ExtractedDocument extract(byte[] content, String documentId, String filename) {
        DocumentFormat format = DocumentFormat.fromFilename(filename)
                .orElseThrow(() -> new DocumentExtractionException( 
                        "Format de document non reconnu a partir du nom de fichier : " + filename
                                + ". Formats acceptes : PDF, DOCX."));
        return extract(content, documentId, filename, format);
    }

    /** Extrait un document dont le format est deja connu, sans se fier au nom de fichier. */
    public ExtractedDocument extract(byte[] content, String documentId, String filename, DocumentFormat format) {
        DocumentExtractor extractor = extractorsByFormat.get(format);
        if (extractor == null) {
            throw new DocumentExtractionException("Aucun extracteur disponible pour le format " + format);
        }
        return extractor.extract(content, documentId, filename);
    }

    /** Formats pour lesquels un extracteur est effectivement enregistre. */
    public List<DocumentFormat> supportedFormats() {
        return List.copyOf(extractorsByFormat.keySet());
    }
}
