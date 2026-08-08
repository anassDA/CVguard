package ma.emsi.cvguard.domain.instruction;

/**
 * Occurrence d'un motif d'instruction au sein d'un texte (O3, regles R5 et R7).
 *
 * @param pattern famille de motifs rencontree
 * @param index    position de la premiere occurrence dans la chaine examinee
 * @param excerpt  portion de texte effectivement appariee, restituee dans le rapport
 */
public record InstructionMatch(InstructionPattern pattern, int index, String excerpt) {
}
