package ma.emsi.cvguard.domain.rule;

import java.util.List;
import ma.emsi.cvguard.domain.model.Detection;
import ma.emsi.cvguard.domain.model.RuleCode;

/**
 * Contrat commun a toutes les regles de detection (O2, ENF-07).
 *
 * <p>Une regle est une unite autonome : elle recoit le contexte d'inspection et
 * retourne la liste de ses signalements, sans connaissance des autres regles ni du
 * calcul du score. Ajouter une regle consiste a publier une nouvelle implementation
 * de cette interface ; le moteur d'orchestration n'a pas a etre modifie.</p>
 */
public interface DetectionRule {

    /** Code de la regle, qui porte son libelle et sa ponderation par defaut. */
    RuleCode code();

    /**
     * Applique la regle au document.
     *
     * <p>Une regle ne doit jamais lever d'exception pour signaler l'absence de
     * detection : elle retourne alors une liste vide. Les exceptions techniques sont
     * neanmoins interceptees par le moteur, conformement a ENF-06.</p>
     */
    List<Detection> apply(RuleContext context);

    /**
     * Indique si la regle est applicable au document soumis.
     *
     * <p>Permet a une regle de se retirer proprement lorsque le document ne porte
     * pas l'information dont elle a besoin — par exemple une regle positionnelle
     * face a un DOCX, dont la pagination n'est pas connue.</p>
     */
    default boolean supports(RuleContext context) {
        return true;
    }
}
