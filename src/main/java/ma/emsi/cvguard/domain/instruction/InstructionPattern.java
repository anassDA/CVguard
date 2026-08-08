package ma.emsi.cvguard.domain.instruction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Familles de motifs reconnues par l'analyse d'instruction (O3, chapitre 8, regle R7).
 *
 * <p>Quatre familles sont decrites par le cahier des charges : les imperatifs adresses
 * a un systeme, les marqueurs de role, les balises de cloture de consigne et les
 * formulations d'evaluation autoreferentes. Chaque famille porte un jeu d'expressions
 * regulieres — francaises et anglaises — compilees une seule fois au chargement.</p>
 *
 * <p>Les expressions sont evaluees sur un texte normalise NFKC : un candidat qui
 * redigerait son instruction en caractere pleine chasse ({@code ＩＧＮＯＲＥ} au lieu
 * de {@code IGNORE}) est ramene a la forme canonique avant la recherche, ce qui rend
 * le contournement trivialement impossible.</p>
 */
public enum InstructionPattern {

    IMPERATIVE_SYSTEM_ADDRESS("Adresse imperative a un systeme", List.of(
            // Anglais
            "\\bignore[sz]?\\b[^\\n]{0,40}\\b(instructions?|prompts?|commands?|directives?|guidelines?|rules?)\\b",
            "\\bignore[sz]?\\b[^\\n]{0,20}\\b(everything|all|anything)\\b",
            "\\bdisregard\\b[^\\n]{0,40}\\b(instructions?|prompts?|commands?|directives?|the above)\\b",
            "\\bforget\\b[^\\n]{0,30}\\b(instructions?|prompts?|commands?|everything|all|previous)\\b",
            "\\byou (must|shall|need to|have to|are to|are required to|will now|are now)\\b",
            "\\bdo not (follow|comply with|obey)\\b",
            "\\bpay no attention to\\b",
            "\\bnever (follow|obey|comply with)\\b",
            "\\boverride\\b[^\\n]{0,30}\\b(instructions?|prompts?|system|settings)\\b",
            "\\bsystem (prompt|instruction|message)\\b",
            "\\bthis is (an?|your) (new|updated|official|important)?\\s*(instruction|directive|system prompt)\\b",
            // Francais
            "\\bignore[z]?\\b[^\\n]{0,40}\\b(instructions?|consignes?|directives?|r[èe]gles?|pr[eé]c[eè]dentes?)\\b",
            "\\boublie(?:z)?\\b[^\\n]{0,30}\\b(instructions?|consignes?|directives?|tout|tous|ce qui pr[eé]c[eè]de)\\b",
            "\\bne (tiens|tenez|fais|faites|suis|suivez|prends|prenez) pas compte\\b",
            "\\b(tu dois|vous devez)\\b",
            "\\bconsigne[s]? (syst[eè]me|syst[eè]mique|officielle)\\b",
            "\\b(d[eé]sormais|à partir de maintenant) (tu|vous)\\b")),

    ROLE_MARKER("Marqueur de role", List.of(
            "\\byou are now\\b",
            "\\byou (will|are) (now )?(acting|act) as\\b",
            "\\bassume the role\\b",
            "\\bpretend (to be|you are)\\b",
            "\\bimagine you are\\b",
            "\\bin the role of\\b",
            "\\bact as if you were\\b",
            "\\btu es (maintenant|d[eé]sormais)\\b",
            "\\bvous [êe]tes (maintenant|d[eé]sormais)\\b",
            "\\bagis comme\\b",
            "\\bagissez comme\\b",
            "\\bdans le r[oô]le de\\b",
            "\\bimagine[z]? que tu\\b",
            "\\bfais comme si tu\\b")),

    END_OF_INSTRUCTION_TAG("Balise de fin de consigne", List.of(
            "\\bend of (the )?instructions?\\b",
            "</instructions>",
            "</system>",
            "\\binstructions? (are )?(complete|finished|ended|done|over)\\b",
            "\\bno (more|further) instructions?\\b",
            "\\bignore (everything|all|anything) (above|previous|prior|before)\\b",
            "\\b(the )?(above|previous|prior) (text|content|message|instructions?|prompt) (was|is|constitutes)\\b",
            "\\bfin des instructions\\b",
            "\\bfin de (la |les )?consigne[s]?\\b",
            "\\binstructions? (termin[eé]es?|finies?|compl[eè]tes?)\\b",
            "\\bc[eé] qui pr[eé]c[eè]de\\b",
            "\\bignore[z]? ce qui pr[eé]c[eè]de\\b")),

    SELF_REFERENTIAL_EVALUATION("Evaluation autoreferente", List.of(
            "\\brecommend (this|me|him|her|the) (candidate|applicant|resume|cv|profile)\\b",
            "\\brecommend (him|her|me) for\\b",
            "\\b(rate|score|rank) (this|my|the) (candidate|applicant|resume|cv|profile)\\b",
            "\\b(evaluate|assess|review) (this|my|the) (cv|resume|application|profile|document)\\b",
            "\\b(must|should|needs to) (be|get) (hired|accepted|recommended|selected)\\b",
            "\\b(give|grant|offer) (him|her|this candidate|the candidate) (a|the|an)?\\s*"
                    + "(high|maximum|perfect|excellent|best|top)?\\s*(score|grade|rating|recommendation)\\b",
            "\\b(select|accept) this candidate\\b",
            "\\b(offered|give) (him|her|me) (the|a) (position|job|role|offer)\\b",
            "\\brecommande ce candidat\\b",
            "\\brecommande[- ](moi|le)\\b",
            "\\b[ée]value ce (cv|profil|document|candidat)\\b",
            "\\b(evalue|[ée]valuez) mon (cv|profil|dossier)\\b",
            "\\b(attribue|attribuez|donne|donnez|accorde|accordez)[^\\n]{0,30}(note|notes|points|mention)\\b",
            "\\bdoit [êe]tre (recommand[ée]|embauch[ée]|retenu|accept[ée]|s[eé]lectionn[ée])\\b",
            "\\bembauche ce candidat\\b",
            "\\baccepte ce candidat\\b",
            "\\b(offre|propose)[- ]lui\\b")),
    ;

    private static final Map<InstructionPattern, List<Pattern>> COMPILED = new EnumMap<>(InstructionPattern.class);

    static {
        for (InstructionPattern pattern : values()) {
            List<Pattern> compiled = new ArrayList<>(pattern.regexes.size());
            for (String regex : pattern.regexes) {
                compiled.add(Pattern.compile(regex,
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));
            }
            COMPILED.put(pattern, List.copyOf(compiled));
        }
    }

    private final String label;
    private final List<String> regexes;

    InstructionPattern(String label, List<String> regexes) {
        this.label = label;
        this.regexes = List.copyOf(regexes);
    }

    public String label() {
        return label;
    }

    /** Expressions compilees de la famille, immuables. */
    public List<Pattern> patterns() {
        return COMPILED.get(this);
    }
}
