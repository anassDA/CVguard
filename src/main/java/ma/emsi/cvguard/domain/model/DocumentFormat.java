package ma.emsi.cvguard.domain.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Formats de document acceptes en entree (EF-01).
 */
public enum DocumentFormat {

    PDF("application/pdf", ".pdf"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");

    private final String contentType;
    private final String extension;

    DocumentFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    /**
     * Deduit le format a partir du nom de fichier.
     *
     * <p>Le type MIME declare par le client n'est volontairement pas la source de
     * verite : il est fourni par l'appelant et donc non fiable. Le format reel est
     * confirme par l'extracteur au moment de l'ouverture du document.</p>
     */
    public static Optional<DocumentFormat> fromFilename(String filename) {
        if (filename == null) {
            return Optional.empty();
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        for (DocumentFormat format : values()) {
            if (lower.endsWith(format.extension)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
