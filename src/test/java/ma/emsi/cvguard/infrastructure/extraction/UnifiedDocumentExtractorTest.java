package ma.emsi.cvguard.infrastructure.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import ma.emsi.cvguard.domain.model.DocumentFormat;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.port.DocumentExtractionException;
import ma.emsi.cvguard.infrastructure.extraction.docx.DocxDocumentExtractor;
import ma.emsi.cvguard.infrastructure.extraction.pdf.PdfDocumentExtractor;
import ma.emsi.cvguard.support.TestDocxBuilder;
import ma.emsi.cvguard.support.TestPdfBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification du point d'entree unifie de l'extraction (livrable J3).
 */
class UnifiedDocumentExtractorTest {

    private final UnifiedDocumentExtractor unified =
            new UnifiedDocumentExtractor(List.of(new PdfDocumentExtractor(), new DocxDocumentExtractor()));

    @Test
    @DisplayName("Un PDF est dispatche vers l'extracteur PDF a partir du nom de fichier")
    void dispatchesPdfByFilename() {
        byte[] pdf = TestPdfBuilder.a4().text("Ingenieur", 72f, 700f).build();

        ExtractedDocument document = unified.extract(pdf, "doc-1", "cv.pdf");

        assertThat(document.format()).isEqualTo(DocumentFormat.PDF);
        assertThat(document.nativeText()).contains("Ingenieur");
    }

    @Test
    @DisplayName("Un DOCX est dispatche vers l'extracteur DOCX a partir du nom de fichier")
    void dispatchesDocxByFilename() {
        byte[] docx = TestDocxBuilder.create().paragraph("Ingenieur").build();

        ExtractedDocument document = unified.extract(docx, "doc-2", "cv.docx");

        assertThat(document.format()).isEqualTo(DocumentFormat.DOCX);
        assertThat(document.nativeText()).contains("Ingenieur");
    }

    @Test
    @DisplayName("La reconnaissance du nom de fichier est insensible a la casse")
    void filenameRecognitionIsCaseInsensitive() {
        byte[] pdf = TestPdfBuilder.a4().build();

        assertThat(unified.extract(pdf, "doc-3", "CV_ANASS.PDF").format()).isEqualTo(DocumentFormat.PDF);
    }

    @Test
    @DisplayName("Un format non reconnu est refuse explicitement")
    void unrecognisedFormatIsRejected() {
        assertThatExceptionOfType(DocumentExtractionException.class)
                .isThrownBy(() -> unified.extract(new byte[0], "doc-4", "cv.txt"))
                .withMessageContaining("cv.txt");
    }

    @Test
    @DisplayName("Le format peut etre impose sans dependre du nom de fichier")
    void formatCanBeSpecifiedExplicitly() {
        byte[] pdf = TestPdfBuilder.a4().text("Contenu", 72f, 700f).build();

        ExtractedDocument document = unified.extract(pdf, "doc-5", "piece-jointe", DocumentFormat.PDF);

        assertThat(document.format()).isEqualTo(DocumentFormat.PDF);
    }

    @Test
    @DisplayName("Les deux formats du perimetre sont enregistres")
    void bothInScopeFormatsAreRegistered() {
        assertThat(unified.supportedFormats()).containsExactlyInAnyOrder(DocumentFormat.PDF, DocumentFormat.DOCX);
    }

    @Test
    @DisplayName("Un format sans extracteur enregistre est refuse explicitement")
    void formatWithoutRegisteredExtractorIsRejected() {
        UnifiedDocumentExtractor onlyPdf = new UnifiedDocumentExtractor(List.of(new PdfDocumentExtractor()));

        assertThatExceptionOfType(DocumentExtractionException.class)
                .isThrownBy(() -> onlyPdf.extract(new byte[0], "doc-6", "cv.docx"));
    }
}
