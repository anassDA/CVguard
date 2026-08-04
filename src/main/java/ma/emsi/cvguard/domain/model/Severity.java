package ma.emsi.cvguard.domain.model;

/**
 * Gravite d'une detection, restituee dans le rapport (EF-11).
 *
 * <p>La gravite qualifie une detection isolee ; elle n'entre pas dans le calcul
 * du score, lequel repose exclusivement sur la ponderation de la regle. Elle sert
 * a l'analyste securite pour hierarchiser sa revue (UC3).</p>
 */
public enum Severity {

    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
