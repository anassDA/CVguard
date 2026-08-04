package ma.emsi.cvguard.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Parametrage du service, expose sous le prefixe {@code cvguard} dans
 * {@code application.yml}.
 *
 * <p>Le cahier des charges impose que seuils et ponderations soient reglables sans
 * recompilation. Cette classe porte les valeurs de demarrage ; leur modification a
 * chaud (UC5, UC6) passe par {@link RuleRegistry}, qui en derive un etat mutable.</p>
 */
@ConfigurationProperties(prefix = "cvguard")
public class CvGuardProperties {

    private final Upload upload = new Upload();
    private final Detection detection = new Detection();
    private final Ocr ocr = new Ocr();
    private final Sanitization sanitization = new Sanitization();

    public Upload getUpload() {
        return upload;
    }

    public Detection getDetection() {
        return detection;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public Sanitization getSanitization() {
        return sanitization;
    }

    /** Contraintes appliquees au fichier soumis (EF-01). */
    public static class Upload {

        private DataSize maxSize = DataSize.ofMegabytes(10);

        public DataSize getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(DataSize maxSize) {
            this.maxSize = maxSize;
        }
    }

    /** Etat initial du moteur de detection. */
    public static class Detection {

        /** Etat d'activation et poids de chaque regle, indexes par code (R1, R2...). */
        private Map<String, RuleConfig> rules = new LinkedHashMap<>();

        private Thresholds thresholds = new Thresholds();

        public Map<String, RuleConfig> getRules() {
            return rules;
        }

        public void setRules(Map<String, RuleConfig> rules) {
            this.rules = rules;
        }

        public Thresholds getThresholds() {
            return thresholds;
        }

        public void setThresholds(Thresholds thresholds) {
            this.thresholds = thresholds;
        }
    }

    /** Reglage d'une regle : un poids nul revient a la neutraliser sans la desactiver. */
    public static class RuleConfig {

        private boolean enabled = true;
        private Integer weight;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }
    }

    /** Seuils de declenchement, repris du chapitre 8 du cahier des charges. */
    public static class Thresholds {

        private float minFontSizePt = 3f;
        private float medianFontRatio = 0.40f;
        private double minContrastRatio = 1.5d;
        private float positionTolerancePt = 2f;
        private double occlusionRatio = 0.90d;
        private double invisibleCharDensity = 0.01d;
        private double ocrSimilarityGap = 0.15d;
        private int suspectScore = 25;
        private int maliciousScore = 60;

        public DetectionThresholds toDomain() {
            return new DetectionThresholds(
                    minFontSizePt,
                    medianFontRatio,
                    minContrastRatio,
                    positionTolerancePt,
                    occlusionRatio,
                    invisibleCharDensity,
                    ocrSimilarityGap,
                    suspectScore,
                    maliciousScore);
        }

        public float getMinFontSizePt() {
            return minFontSizePt;
        }

        public void setMinFontSizePt(float minFontSizePt) {
            this.minFontSizePt = minFontSizePt;
        }

        public float getMedianFontRatio() {
            return medianFontRatio;
        }

        public void setMedianFontRatio(float medianFontRatio) {
            this.medianFontRatio = medianFontRatio;
        }

        public double getMinContrastRatio() {
            return minContrastRatio;
        }

        public void setMinContrastRatio(double minContrastRatio) {
            this.minContrastRatio = minContrastRatio;
        }

        public float getPositionTolerancePt() {
            return positionTolerancePt;
        }

        public void setPositionTolerancePt(float positionTolerancePt) {
            this.positionTolerancePt = positionTolerancePt;
        }

        public double getOcclusionRatio() {
            return occlusionRatio;
        }

        public void setOcclusionRatio(double occlusionRatio) {
            this.occlusionRatio = occlusionRatio;
        }

        public double getInvisibleCharDensity() {
            return invisibleCharDensity;
        }

        public void setInvisibleCharDensity(double invisibleCharDensity) {
            this.invisibleCharDensity = invisibleCharDensity;
        }

        public double getOcrSimilarityGap() {
            return ocrSimilarityGap;
        }

        public void setOcrSimilarityGap(double ocrSimilarityGap) {
            this.ocrSimilarityGap = ocrSimilarityGap;
        }

        public int getSuspectScore() {
            return suspectScore;
        }

        public void setSuspectScore(int suspectScore) {
            this.suspectScore = suspectScore;
        }

        public int getMaliciousScore() {
            return maliciousScore;
        }

        public void setMaliciousScore(int maliciousScore) {
            this.maliciousScore = maliciousScore;
        }
    }

    /** Parametres du controle OCR (regle R6). */
    public static class Ocr {

        /** Desactivable : le service reste demontrable sans Tesseract. */
        private boolean enabled = true;

        /** Langues chargees par Tesseract. */
        private String language = "fra+eng";

        /** Resolution de rendu des pages, compromis entre fidelite et cout. */
        private int renderDpi = 150;

        /** Repertoire des donnees de langue ; laisse vide, la variable TESSDATA_PREFIX fait foi. */
        private String dataPath;

        /** Nombre maximal de pages soumises a l'OCR, pour borner le cout (ENF-01). */
        private int maxPages = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public int getRenderDpi() {
            return renderDpi;
        }

        public void setRenderDpi(int renderDpi) {
            this.renderDpi = renderDpi;
        }

        public String getDataPath() {
            return dataPath;
        }

        public void setDataPath(String dataPath) {
            this.dataPath = dataPath;
        }

        public int getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(int maxPages) {
            this.maxPages = maxPages;
        }
    }

    /** Parametres de la politique d'assainissement (chapitre 8.2). */
    public static class Sanitization {

        /** Nom de la balise encapsulant le contenu transmis au modele. */
        private String envelopeTag = "candidate_document";

        /** Longueur maximale de l'extrait restitue dans le rapport, pour ne pas le rendre exploitable. */
        private int maxExcerptLength = 120;

        public String getEnvelopeTag() {
            return envelopeTag;
        }

        public void setEnvelopeTag(String envelopeTag) {
            this.envelopeTag = envelopeTag;
        }

        public int getMaxExcerptLength() {
            return maxExcerptLength;
        }

        public void setMaxExcerptLength(int maxExcerptLength) {
            this.maxExcerptLength = maxExcerptLength;
        }
    }

    /** Types MIME acceptes, fournis a titre indicatif par la couche d'exposition. */
    public static final List<String> ACCEPTED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream");
}
