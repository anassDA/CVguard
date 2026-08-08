package ma.emsi.cvguard.exposition;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import ma.emsi.cvguard.application.CorrelationIds;
import ma.emsi.cvguard.application.InspectionOutcome;
import ma.emsi.cvguard.application.InspectionReport;
import ma.emsi.cvguard.application.InspectionService;
import ma.emsi.cvguard.config.CvGuardProperties;
import ma.emsi.cvguard.exposition.dto.SanitizeResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Point d'entree de l'inspection (UC1, UC2) : soumission d'un document PDF ou DOCX et
 * restitution du rapport de detection, ou de la version assainie.
 *
 * <p>Le document est traite integralement en memoire (ENF-05) : aucun fichier temporaire
 * n'est ecrit sur disque, et le fichier est libere a l'issue du traitement. Un
 * identifiant de correlation (EF-14) est attribue a chaque inspection et porte par les
 * journaux et le rapport.</p>
 */
@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Inspection", description = "Inspection et assainissement des documents de candidature (UC1, UC2)")
public class InspectionController {

    private final InspectionService service;
    private final CvGuardProperties properties;

    public InspectionController(InspectionService service, CvGuardProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping(value = "/inspect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Inspecter un document",
            description = "Soumet un PDF ou un DOCX et retourne le rapport de detection complet : "
                    + "verdict, score, detections localisees, controle OCR et mesures d'assainissement.")
    public InspectionReport inspect(@RequestParam("file") MultipartFile file) {
        return withCorrelation(file, InspectionOutcome::report);
    }

    @PostMapping(value = "/sanitize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Assainir un document",
            description = "Retourne la version assainie du document : segments incrimines purges, "
                    + "caracteres de controle retires, contenu encapsule dans des delimiteurs explicites.")
    public SanitizeResponse sanitize(@RequestParam("file") MultipartFile file) {
        return withCorrelation(file, outcome -> new SanitizeResponse(
                outcome.report().documentId(),
                outcome.report().filename(),
                outcome.report().format(),
                outcome.verdict(),
                outcome.report().riskScore(),
                outcome.sanitized().removedSegments(),
                properties.getSanitization().getEnvelopeTag(),
                properties.getLlm().getSystemInstruction(),
                outcome.sanitized().content()));
    }

    /**
     * Attribue l'identifiant de correlation (EF-14), l'expose aux journaux via MDC,
     * valide la requete puis execute le traitement. L'identifiant est genere avant le
     * traitement afin qu'une erreur d'extraction soit elle-meme correlable.
     */
    private <T> T withCorrelation(MultipartFile file, java.util.function.Function<InspectionOutcome, T> mapper) {
        String correlationId = CorrelationIds.newId();
        MDC.put("correlationId", correlationId);
        try {
            validate(file);
            return mapper.apply(service.analyze(file.getBytes(), filenameOf(file)));
        } catch (IOException unreadable) {
            throw new IllegalArgumentException("Le fichier soumis n'a pas pu etre lu.", unreadable);
        } finally {
            MDC.clear();
        }
    }

    private static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Aucun document fourni : joindre un fichier PDF ou DOCX.");
        }
        String filename = filenameOf(file);
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new IllegalArgumentException("Le nom du fichier doit porter l'extension .pdf ou .docx.");
        }
    }

    private static String filenameOf(MultipartFile file) {
        return file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
    }
}
