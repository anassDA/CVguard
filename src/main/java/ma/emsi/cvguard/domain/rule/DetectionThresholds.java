package ma.emsi.cvguard.domain.rule;

/**
 * Seuils de declenchement des regles, externalises pour permettre un reglage
 * sans recompilation (chapitre 8 du cahier des charges).
 *
 * @param minFontSizePt        R1 : corps en deca duquel un fragment est signale (defaut 3 pt)
 * @param medianFontRatio      R1 : fraction du corps median en deca de laquelle un fragment
 *                             est signale, meme s'il depasse {@code minFontSizePt} (defaut 0,40)
 * @param minContrastRatio     R2 : ratio de contraste WCAG minimal admis (defaut 1,5:1)
 * @param positionTolerancePt  R3 : tolerance appliquee aux bords de la zone visible, pour
 *                             absorber les debordements typographiques legitimes
 * @param occlusionRatio       R3 : fraction de la boite recouverte par un element opaque
 *                             au-dela de laquelle le fragment est considere masque
 * @param invisibleCharDensity R4 : densite de caracteres invisibles declenchant un signalement
 * @param ocrSimilarityGap     R6 : ecart de similarite au-dela duquel le texte natif est
 *                             juge diverger du texte reellement visible (defaut 0,15)
 * @param suspectScore         seuil du verdict SUSPECT (defaut 25)
 * @param maliciousScore       seuil du verdict MALICIOUS (defaut 60)
 */
public record DetectionThresholds(
        float minFontSizePt,
        float medianFontRatio,
        double minContrastRatio,
        float positionTolerancePt,
        double occlusionRatio,
        double invisibleCharDensity,
        double ocrSimilarityGap,
        int suspectScore,
        int maliciousScore) {

    public DetectionThresholds {
        if (suspectScore >= maliciousScore) {
            throw new IllegalArgumentException(
                    "Le seuil SUSPECT (" + suspectScore + ") doit rester inferieur au seuil MALICIOUS ("
                            + maliciousScore + ") : sans zone intermediaire, le systeme ecarterait seul "
                            + "des candidatures dans la zone d'incertitude.");
        }
    }

    /** Valeurs de reference du cahier des charges. */
    public static DetectionThresholds defaults() {
        return new DetectionThresholds(3f, 0.40f, 1.5d, 2f, 0.90d, 0.01d, 0.15d, 25, 60);
    }
}
