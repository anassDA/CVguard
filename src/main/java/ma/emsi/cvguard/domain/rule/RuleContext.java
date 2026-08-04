package ma.emsi.cvguard.domain.rule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ma.emsi.cvguard.domain.model.ExtractedDocument;
import ma.emsi.cvguard.domain.model.TextFragment;

/**
 * Contexte transmis a chaque regle : le document extrait, les seuils en vigueur,
 * et un espace de faits partages entre regles.
 *
 * <p>Les grandeurs derivees couteuses — le corps median notamment — sont calculees
 * une seule fois puis memorisees : sept regles parcourant chacune l'integralite des
 * fragments, il serait inutile de les recalculer.</p>
 *
 * <p>L'espace de faits partages evite qu'une regle en depende d'une autre par
 * construction : la regle R6 y depose le texte issu de l'OCR, qu'une regle ulterieure
 * peut consulter sans que l'ordre d'execution soit impose.</p>
 */
public final class RuleContext {

    /** Cle sous laquelle l'etape de controle OCR depose le texte reconnu. */
    public static final String FACT_OCR_TEXT = "ocr.text";

    /** Cle sous laquelle l'etape de controle OCR depose la similarite mesuree. */
    public static final String FACT_OCR_SIMILARITY = "ocr.similarity";

    private final ExtractedDocument document;
    private final DetectionThresholds thresholds;
    private final Map<String, Object> facts = new ConcurrentHashMap<>();

    private volatile Double medianFontSizePt;

    public RuleContext(ExtractedDocument document, DetectionThresholds thresholds) {
        this.document = document;
        this.thresholds = thresholds;
    }

    public ExtractedDocument document() {
        return document;
    }

    public DetectionThresholds thresholds() {
        return thresholds;
    }

    /**
     * Corps median du document, pondere par le nombre de caracteres.
     *
     * <p>La ponderation par la longueur est deliberee : un CV comporte souvent un
     * titre en corps 24 et deux lignes de mentions legales en corps 6. Une mediane
     * calculee sur les fragments donnerait le meme poids a chacun, alors que la
     * mediane ponderee restitue le corps du texte courant, seule reference pertinente
     * pour juger qu'un fragment est anormalement petit.</p>
     *
     * @return le corps median en points, ou 0 si le document ne contient aucun fragment rendu
     */
    public double medianFontSizePt() {
        Double cached = medianFontSizePt;
        if (cached == null) {
            synchronized (this) {
                if (medianFontSizePt == null) {
                    medianFontSizePt = computeWeightedMedianFontSize();
                }
                cached = medianFontSizePt;
            }
        }
        return cached;
    }

    public void putFact(String key, Object value) {
        if (value != null) {
            facts.put(key, value);
        }
    }

    public <T> Optional<T> fact(String key, Class<T> type) {
        Object value = facts.get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    private double computeWeightedMedianFontSize() {
        List<TextFragment> rendered = document.renderedFragments().stream()
                .filter(fragment -> !fragment.isBlank())
                .sorted((left, right) -> Float.compare(left.fontSizePt(), right.fontSizePt()))
                .toList();
        if (rendered.isEmpty()) {
            return 0d;
        }

        long totalChars = rendered.stream().mapToLong(fragment -> fragment.text().strip().length()).sum();
        if (totalChars == 0L) {
            return rendered.get(rendered.size() / 2).fontSizePt();
        }

        long half = totalChars / 2;
        long cumulated = 0L;
        for (TextFragment fragment : rendered) {
            cumulated += fragment.text().strip().length();
            if (cumulated > half) {
                return fragment.fontSizePt();
            }
        }
        return rendered.get(rendered.size() - 1).fontSizePt();
    }
}
