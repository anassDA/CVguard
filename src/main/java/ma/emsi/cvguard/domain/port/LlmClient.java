package ma.emsi.cvguard.domain.port;

/**
 * Port d'appel a un modele de langage, utilise par le seul demonstrateur comparatif
 * (EF-13).
 *
 * <p>Ce port n'appartient pas a la chaine d'inspection : CVGuard n'a pas besoin d'un
 * modele pour rendre son verdict. Il sert uniquement a etablir experimentalement
 * l'efficacite de la contre-mesure (O6), en soumettant le meme document a un pipeline
 * naif puis a un pipeline protege.</p>
 *
 * <p>Conformement a ENF-03, seule la version assainie du contenu franchit cette
 * frontiere dans le pipeline protege.</p>
 */
public interface LlmClient {

    /** Indique si un modele est configure et joignable. */
    boolean isAvailable();

    /**
     * Soumet une consigne systeme et un contenu au modele.
     *
     * @param systemInstruction consigne d'evaluation, rediger par l'appelant
     * @param content           contenu a analyser
     * @return la reponse brute du modele
     */
    String evaluate(String systemInstruction, String content);
}
