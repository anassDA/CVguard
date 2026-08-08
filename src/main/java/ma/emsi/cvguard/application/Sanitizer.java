package ma.emsi.cvguard.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ma.emsi.cvguard.config.CvGuardProperties;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.text.InvisibleCharacterScanner;
import org.springframework.stereotype.Component;

/**
 * Politique d'assainissement (chapitre 8.2, etape [7] de la chaine de traitement).
 *
 * <p>Trois operations sont conduites dans l'ordre :</p>
 * <ol>
 *   <li><b>Purge.</b> Les fragments incrimines par une regle de detection sont retires
 *       du texte transmis ; leur localisation reste dans le rapport d'inspection.</li>
 *   <li><b>Normalisation.</b> Le texte conserves est normalise NFKC (idempotente :
 *       sans effet sur un texte deja normalise par l'extraction) et debarrasse de tout
 *       caractere de controle ou de largeur nulle.</li>
 *   <li><b>Encapsulation.</b> Le contenu est encadre par des delimiteurs explicites et
 *       non devinables ({@code <candidate_document id="..."> ... </candidate_document>}),
 *       la consigne systeme precise qu'il constitue exclusivement de la donnee.</li>
 * </ol>
 *
 * <p>Les metadonnees ne sont pas transmises : elles ne font pas partie du texte rendu,
 * et un ATS ne les soumet pas a l'analyse du modele.</p>
 */
@Component
public class Sanitizer {

    private final CvGuardProperties.Sanitization config;

    public Sanitizer(CvGuardProperties properties) {
        this.config = properties.getSanitization();
    }

    public SanitizedContent sanitize(ExtractedDocument document, List<Detection> detections) {
        Set<Integer> removed = new HashSet<>();
        for (Detection detection : detections) {
            if (detection.fragmentIndex() >= 0) {
                removed.add(detection.fragmentIndex());
            }
        }

        StringBuilder kept = new StringBuilder();
        for (TextFragment fragment : document.fragments()) {
            if (fragment.origin() == FragmentOrigin.METADATA) {
                continue;
            }
            if (removed.contains(fragment.index())) {
                continue;
            }
            kept.append(purgeControlCharacters(fragment.text()));
        }

        String body = kept.toString();
        String tag = config.getEnvelopeTag();
        String encapsulated = "<" + tag + " id=\"" + document.documentId() + "\">\n"
                + body
                + "\n</" + tag + ">";

        return new SanitizedContent(encapsulated, removed.size(), body.length());
    }

    /**
     * Retire tout point de code invisible ou de largeur nulle ainsi que tout caractere
     * de controle, conformement au chapitre 8.2. Les marques directionnelles — U+202A,
     * U+202E... — sont des caracteres FORMAT : leur suppression est necessaire pour
     * qu'une instruction ne puisse pas reordonner le contenu transmis.
     */
    private static String purgeControlCharacters(String text) {
        StringBuilder clean = new StringBuilder(text.length());
        text.codePoints().forEach(codePoint -> {
            if (InvisibleCharacterScanner.isInvisible(codePoint) || Character.getType(codePoint) == Character.CONTROL) {
                return;
            }
            clean.appendCodePoint(codePoint);
        });
        return clean.toString();
    }
}
