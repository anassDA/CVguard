package ma.emsi.cvguard.domain.rule;

import ma.emsi.cvguard.domain.model.RuleCode;

/**
 * Trace d'une regle ayant echoue (ENF-06).
 *
 * <p>L'echec d'une regle n'interrompt pas l'inspection, mais il ne doit pas non plus
 * etre silencieux : un document declare SAFE alors qu'une regle a echoue n'a pas la
 * meme valeur qu'un document declare SAFE par un moteur complet. L'information est
 * donc restituee dans le rapport.</p>
 *
 * @param rule   regle concernee
 * @param reason cause de l'echec, sans trace d'execution
 */
public record RuleFailure(RuleCode rule, String reason) {
}
