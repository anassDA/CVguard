package ma.emsi.cvguard.domain.rule.impl;

import java.util.ArrayList;
import java.util.List;
import ma.emsi.cvguard.domain.instruction.InstructionAnalyzer;
import ma.emsi.cvguard.domain.instruction.InstructionMatch;
import ma.emsi.cvguard.domain.instruction.InstructionPattern;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.FragmentOrigin;
import ma.emsi.cvguard.domain.model.RuleCode;
import ma.emsi.cvguard.domain.model.TextFragment;
import ma.emsi.cvguard.domain.rule.DetectionRule;
import ma.emsi.cvguard.domain.rule.RuleContext;
import org.springframework.stereotype.Component;

/**
 * R7 — Structure d'instruction (EF-14, O3, chapitre 8).
 *
 * <p>Recherche dans le contenu visible du document les segments rediges sous forme
 * d'instruction adressee a un systeme : imperatifs (« ignore les instructions
 * precedentes »), marqueurs de role (« tu es maintenant un evalueur »), balises de
 * cloture de consigne (« fin des instructions », {@code </instructions>}) et
 * formulations d'evaluation autoreferentes (« recommande ce candidat »). Ces
 * formulations n'ont aucune raison d'apparaitre dans un CV — elles ne decrivent pas
 * une experience, elles donnent un ordre au lecteur.</p>
 *
 * <p>L'analyse est celle de {@link InstructionAnalyzer}, source unique des motifs ;
 * elle s'applique ici aux fragments rendus (corps, en-tetes, pieds de page) et aux
 * textes alternatifs, la regle R5 traitant les metadonnees et les commentaires.</p>
 */
@Component
public class InstructionContentRule implements DetectionRule {

    @Override
    public RuleCode code() {
        return RuleCode.R7;
    }

    @Override
    public List<Detection> apply(RuleContext context) {
        List<Detection> detections = new ArrayList<>();
        for (TextFragment fragment : context.document().fragments()) {
            if (!isVisibleOrigin(fragment.origin())) {
                continue;
            }
            List<InstructionMatch> matches = InstructionAnalyzer.analyze(fragment.text());
            if (matches.isEmpty()) {
                continue;
            }

            List<InstructionPattern> patterns = matches.stream()
                    .map(InstructionMatch::pattern)
                    .distinct()
                    .toList();

            detections.add(Detection.of(RuleCode.R7)
                    .at(fragment)
                    .detail("matchCount", matches.size())
                    .detail("matchedPattern", patterns.get(0).name())
                    .detail("matchedPatterns", patterns.stream().map(InstructionPattern::name).toList())
                    .detail("samples", matches.stream()
                            .limit(3)
                            .map(InstructionMatch::excerpt)
                            .toList())
                    .build());
        }
        return detections;
    }

    private static boolean isVisibleOrigin(FragmentOrigin origin) {
        return origin == FragmentOrigin.BODY
                || origin == FragmentOrigin.HEADER
                || origin == FragmentOrigin.FOOTER
                || origin == FragmentOrigin.ALT_TEXT;
    }
}
