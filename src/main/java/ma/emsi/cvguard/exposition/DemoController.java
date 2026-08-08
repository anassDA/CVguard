package ma.emsi.cvguard.exposition;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import ma.emsi.cvguard.application.CorrelationIds;
import ma.emsi.cvguard.application.DemoReport;
import ma.emsi.cvguard.application.DemoService;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Demonstrateur comparatif (EF-13, O6) : le meme document est soumis au pipeline naif
 * puis au pipeline protege, et la reponse du modele est restituee cote a cote.
 *
 * <p>La page de demonstration (interface statique servie par Nginx, profil Docker
 * {@code demo}) consomme ce point d'entree.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/demo", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Demonstration", description = "Comparaison du pipeline naif et du pipeline protege (EF-13)")
public class DemoController {

    private final DemoService service;

    public DemoController(DemoService service) {
        this.service = service;
    }

    @PostMapping(value = "/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Comparer les deux pipelines",
            description = "Soumet un document, execute successivement le pipeline naif (texte brut "
                    + "transmis au modele) et le pipeline protege (texte assaini et encapsule), et "
                    + "restitue les deux reponses du modele.")
    public DemoReport compare(@RequestParam("file") MultipartFile file) {
        String correlationId = CorrelationIds.newId();
        MDC.put("correlationId", correlationId);
        try {
            validate(file);
            return service.compare(file.getBytes(), filenameOf(file));
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
    }

    private static String filenameOf(MultipartFile file) {
        return file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename();
    }
}
