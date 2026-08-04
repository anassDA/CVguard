package ma.emsi.cvguard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation OpenAPI (livrable L4).
 *
 * <p>L'interface Swagger tient lieu de console d'administration et de banc d'essai :
 * le cahier des charges ecarte explicitement tout developpement front-end lourd.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cvGuardOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CVGuard — API d'inspection de documents de candidature")
                .version("1.0.0")
                .description("""
                        Micro-service d'inspection place en amont de tout appel a un modele de langage.

                        Il detecte, qualifie et neutralise les tentatives d'injection de prompt dissimulees \
                        dans un CV au format PDF ou DOCX : texte a contraste nul, corps typographique \
                        minuscule, positionnement hors zone, caracteres de largeur nulle, metadonnees \
                        piegees et segments rediges sous forme d'instruction.

                        Referentiel : OWASP Top 10 for LLM Applications — LLM01 Prompt Injection.""")
                .contact(new Contact().name("Anass DADA — EMSI Rabat"))
                .license(new License().name("Projet personnel — demonstrateur technique")));
    }
}
