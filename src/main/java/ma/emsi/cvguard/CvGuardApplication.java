package ma.emsi.cvguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d'entree du micro-service CVGuard.
 *
 * <p>Le service inspecte un document de candidature (PDF ou DOCX) en amont de tout appel
 * a un modele de langage, afin de detecter et de neutraliser les tentatives d'injection
 * de prompt (OWASP Top 10 for LLM Applications, LLM01).</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CvGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(CvGuardApplication.class, args);
    }
}
