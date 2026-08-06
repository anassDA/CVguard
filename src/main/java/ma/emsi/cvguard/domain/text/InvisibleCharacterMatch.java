package ma.emsi.cvguard.domain.text;

/**
 * Occurrence d'un caractere invisible ou d'une marque directionnelle au sein d'un texte
 * (regle R4).
 *
 * @param index      position du caractere dans la chaine examinee (unite {@code char})
 * @param codePoint  point de code Unicode rencontre
 * @param name       designation Unicode courte, a des fins de restitution dans le rapport
 */
public record InvisibleCharacterMatch(int index, int codePoint, String name) {

    public String hex() {
        return String.format("U+%04X", codePoint);
    }
}
