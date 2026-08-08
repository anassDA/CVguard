package ma.emsi.cvguard.infrastructure.llm;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import ma.emsi.cvguard.config.CvGuardProperties;
import ma.emsi.cvguard.domain.port.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Client LLM compatible avec l'interface {@code /chat/completions} d'OpenAI, utilise
 * par le demonstrateur comparatif (EF-13) lorsqu'un modele est configure.
 *
 * <p>Le pipeline protege est le seul a franchir cette frontiere avec du contenu
 * documentaire (ENF-03) : le pipeline naif du demonstrateur est une simulation du
 * comportement expose d'un ATS sans defense — il transmet le texte brut du document.</p>
 */
public class HttpLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(HttpLlmClient.class);

    private final CvGuardProperties.Llm config;
    private final RestClient client;

    public HttpLlmClient(CvGuardProperties.Llm config) {
        this.config = config;
        this.client = RestClient.builder()
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                    {
                        setConnectTimeout(Duration.ofMillis(config.getTimeoutMs()));
                        setReadTimeout(Duration.ofMillis(config.getTimeoutMs()));
                    }
                })
                .build();
    }

    @Override
    public boolean isAvailable() {
        return config.getBaseUrl() != null && !config.getBaseUrl().isBlank();
    }

    @Override
    public String evaluate(String systemInstruction, String content) {
        if (!isAvailable()) {
            throw new IllegalStateException("Aucune URL de modele configuree (cvguard.llm.base-url)");
        }
        Map<String, Object> payload = Map.of(
                "model", config.getModel(),
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", systemInstruction),
                        Map.of("role", "user", "content", content)));

        ChatCompletionResponse response = client.post()
                .uri(config.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + (config.getApiKey() == null ? "" : config.getApiKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            throw new IllegalStateException("Reponse du modele vide ou non conforme au schema attendu");
        }
        String text = response.choices().get(0).message().content();
        log.info("Reponse du modele recue : {} caractere(s)", text == null ? 0 : text.length());
        return text;
    }

    /** Enveloppe minimale de la reponse {@code /chat/completions}. */
    public record ChatCompletionResponse(List<Choice> choices) {
    }

    public record Choice(ChatMessage message) {
    }

    public record ChatMessage(String content) {
    }
}
