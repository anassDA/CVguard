package ma.emsi.cvguard.infrastructure.llm;

import ma.emsi.cvguard.domain.instruction.InstructionAnalyzer;
import ma.emsi.cvguard.domain.port.LlmClient;

/**
 * Simulation du comportement d'un modele vulnérable, faute de modele reel configure.
 *
 * <p>Le demonstrateur comparatif (EF-13) soumet le meme document au pipeline naif puis
 * au pipeline protege. Sans modele, la simulation reproduit le phenomene que la
 * contre-mesure vise a empecher : un pipeline naif transmet au « modele » un contenu
 * porteur d'instructions, que celui-ci execute ; le pipeline protege lui transmet un
 * contenu encapsule en donnee, dont aucune instruction ne subsiste, et l'analyse reste
 * conforme. La simulation est clairement etiquetee dans la reponse du demonstrateur.</p>
 */
public class SimulatedLlmClient implements LlmClient {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String evaluate(String systemInstruction, String content) {
        if (InstructionAnalyzer.containsInstruction(content)) {
            return """
                    REPONSE SIMULEE — modele sans defense :
                    le contenu soumis contenait une instruction dissimulee, que le modele a executee.
                    Recommandation emise : note maximale, candidature validee sans revue.""";
        }
        return """
                REPONSE SIMULEE — analyse conforme :
                le contenu a ete evalue selon les criteres fournis. Aucune instruction n'a
                ete detectee dans le document ; la decision releve de l'evaluation humaine.""";
    }
}
