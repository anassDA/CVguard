package ma.emsi.cvguard.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Signalement produit par une regle (EF-11, ENF-10).
 *
 * <p>Chaque detection reference explicitement la regle qui l'a produite, afin que
 * le verdict reste explicable : un analyste doit pouvoir remonter d'un score a la
 * ligne de texte et au critere qui l'ont declenche.</p>
 *
 * @param rule        regle a l'origine du signalement
 * @param severity    gravite de ce signalement pris isolement
 * @param page        page concernee, ou 0 lorsque la detection ne porte pas sur une page
 * @param boundingBox localisation sur la page, {@code null} pour une detection non localisable
 * @param excerpt     extrait du texte incrimine, tel que detecte
 * @param details     elements chiffres propres a la regle (ratio de contraste, corps mesure...)
 */
public record Detection(
        RuleCode rule,
        Severity severity,
        int page,
        BoundingBox boundingBox,
        String excerpt,
        Map<String, Object> details) {

    public Detection {
        Objects.requireNonNull(rule, "rule");
        severity = severity == null ? rule.defaultSeverity() : severity;
        excerpt = excerpt == null ? "" : excerpt;
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    /** Libelle lisible de la regle, restitue dans le rapport JSON. */
    public String label() {
        return rule.label();
    }

    public static Builder of(RuleCode rule) {
        return new Builder(rule);
    }

    /**
     * Constructeur fluide : les regles produisent des detections dont seuls
     * quelques champs sont pertinents, ce qui rend le constructeur canonique
     * peu lisible a l'usage.
     */
    public static final class Builder {

        private final RuleCode rule;
        private final Map<String, Object> details = new LinkedHashMap<>();
        private Severity severity;
        private int page;
        private BoundingBox boundingBox;
        private String excerpt = "";

        private Builder(RuleCode rule) {
            this.rule = Objects.requireNonNull(rule, "rule");
            this.severity = rule.defaultSeverity();
        }

        public Builder severity(Severity value) {
            this.severity = value;
            return this;
        }

        public Builder page(int value) {
            this.page = value;
            return this;
        }

        public Builder boundingBox(BoundingBox value) {
            this.boundingBox = value;
            return this;
        }

        public Builder excerpt(String value) {
            this.excerpt = value;
            return this;
        }

        /** Localise la detection a partir du fragment qui l'a declenchee. */
        public Builder at(TextFragment fragment) {
            this.page = fragment.page();
            this.boundingBox = fragment.box();
            this.excerpt = fragment.text();
            return this;
        }

        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public Detection build() {
            return new Detection(rule, severity, page, boundingBox, excerpt, details);
        }
    }
}
