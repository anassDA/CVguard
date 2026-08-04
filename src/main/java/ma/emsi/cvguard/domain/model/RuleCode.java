package ma.emsi.cvguard.domain.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Catalogue des regles de detection et de leur ponderation par defaut,
 * conformement au chapitre 8 du cahier des charges.
 *
 * <p>Le poids porte ici n'est qu'une valeur de repli : la ponderation effective
 * est externalisee en configuration et modifiable a chaud (UC6), sans recompilation.</p>
 */
public enum RuleCode {

    R1("Corps typographique anormal", 25, Severity.HIGH),
    R2("Contraste insuffisant", 30, Severity.HIGH),
    R3("Positionnement hors zone", 20, Severity.MEDIUM),
    R4("Caracteres invisibles", 20, Severity.MEDIUM),
    R5("Metadonnees et commentaires", 10, Severity.LOW),
    R6("Ecart texte extrait / texte rendu", 35, Severity.HIGH),
    R7("Structure d'instruction", 40, Severity.CRITICAL);

    private final String label;
    private final int defaultWeight;
    private final Severity defaultSeverity;

    RuleCode(String label, int defaultWeight, Severity defaultSeverity) {
        this.label = label;
        this.defaultWeight = defaultWeight;
        this.defaultSeverity = defaultSeverity;
    }

    public String label() {
        return label;
    }

    public int defaultWeight() {
        return defaultWeight;
    }

    public Severity defaultSeverity() {
        return defaultSeverity;
    }

    /** Resolution insensible a la casse, pour les parametres d'URL (PUT /api/v1/rules/{code}). */
    public static Optional<RuleCode> from(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(code.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException notARuleCode) {
            return Optional.empty();
        }
    }
}
