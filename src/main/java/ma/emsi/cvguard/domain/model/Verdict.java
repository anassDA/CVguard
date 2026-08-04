package ma.emsi.cvguard.domain.model;

/**
 * Verdict rendu par l'agregateur de score (EF-10).
 */
public enum Verdict {

    /** Aucun signal significatif : le document peut etre transmis au modele. */
    SAFE,

    /**
     * Zone d'incertitude : le document est transmis assaini, mais une revue humaine
     * est requise avant toute decision sur la candidature. Ce palier materialise le
     * principe de maintien de l'humain dans la boucle.
     */
    SUSPECT,

    /** Faisceau d'indices concordant : le document est rejete. */
    MALICIOUS
}
