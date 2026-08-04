package ma.emsi.cvguard.config;

import ma.emsi.cvguard.domain.rule.DetectionRule;
import ma.emsi.cvguard.domain.rule.DetectionThresholds;
import ma.emsi.cvguard.domain.rule.RuleEngine;
import ma.emsi.cvguard.domain.score.ScoreAggregator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemblage des composants du domaine.
 *
 * <p>Le moteur de regles et l'agregateur de score sont des classes ordinaires, sans
 * annotation ni dependance au conteneur : c'est ici, et ici seulement, que le domaine
 * est relie a Spring. Le moteur reste ainsi instanciable directement dans les tests
 * unitaires, sans demarrer de contexte applicatif.</p>
 *
 * <p>L'injection de {@code List<DetectionRule>} est le mecanisme d'extensibilite prevu
 * par ENF-07 : toute nouvelle regle publiee comme composant rejoint automatiquement le
 * moteur.</p>
 */
@Configuration
public class DomainConfiguration {

    @Bean
    public DetectionThresholds detectionThresholds(CvGuardProperties properties) {
        return properties.getDetection().getThresholds().toDomain();
    }

    @Bean
    public RuleEngine ruleEngine(ObjectProvider<DetectionRule> rules, RuleRegistry registry) {
        return new RuleEngine(rules.stream().toList(), registry);
    }

    @Bean
    public ScoreAggregator scoreAggregator(RuleRegistry registry, DetectionThresholds thresholds) {
        return new ScoreAggregator(registry, thresholds);
    }
}
