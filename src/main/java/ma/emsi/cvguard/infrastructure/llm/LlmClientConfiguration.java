package ma.emsi.cvguard.infrastructure.llm;

import ma.emsi.cvguard.config.CvGuardProperties;
import ma.emsi.cvguard.domain.port.LlmClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selection du client LLM en fonction de la configuration.
 *
 * <p>Le mode {@code simulated} (defaut) rend le demonstrateur exploitable sans aucun
 * appel reseau ni cle d'API ; le mode {@code http} interroge un service compatible
 * {@code /chat/completions} lorsque la contre-mesure est mesuree sur un modele reel
 * (O6). Dans les deux cas, seul le demonstrateur comparatif appelle ce composant :
 * l'inspection n'a jamais besoin d'un modele pour rendre son verdict.</p>
 */
@Configuration
public class LlmClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "cvguard.llm.mode", havingValue = "http")
    public LlmClient httpLlmClient(CvGuardProperties properties) {
        return new HttpLlmClient(properties.getLlm());
    }

    @Bean
    @ConditionalOnProperty(name = "cvguard.llm.mode", havingValue = "simulated", matchIfMissing = true)
    public LlmClient simulatedLlmClient() {
        return new SimulatedLlmClient();
    }
}
