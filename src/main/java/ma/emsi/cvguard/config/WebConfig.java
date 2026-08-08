package ma.emsi.cvguard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS du demonstrateur.
 *
 * <p>La page de demonstration est servie par Nginx sur un port distinct (8081) du
 * service (8080) : sans CORS, le navigateur refuserait ses appels a l'API. Seules les
 * origines declarees en configuration sont admises — la page de production est hors
 * perimetre, et un acces depuis une origine tierce n'a aucune raison d'etre tolere.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CvGuardProperties properties;

    public WebConfig(CvGuardProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.getCors().getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("*");
    }
}
