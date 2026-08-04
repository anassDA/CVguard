package ma.emsi.cvguard.exposition;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ma.emsi.cvguard.config.RuleRegistry;
import ma.emsi.cvguard.domain.model.RuleCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verification des cas d'utilisation d'administration UC5 et UC6.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RuleRegistry registry;

    @AfterEach
    void restoreDeclaredConfiguration() {
        for (RuleCode code : RuleCode.values()) {
            registry.update(code, true, code.defaultWeight());
        }
    }

    @Test
    @DisplayName("GET /api/v1/rules expose le catalogue complet des sept regles")
    void listsAllRules() throws Exception {
        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(7)))
                .andExpect(jsonPath("$[0].code").value("R1"))
                .andExpect(jsonPath("$[6].code").value("R7"))
                .andExpect(jsonPath("$[6].label").value("Structure d'instruction"))
                .andExpect(jsonPath("$[6].weight").value(40))
                .andExpect(jsonPath("$[6].defaultWeight").value(40));
    }

    @Test
    @DisplayName("UC5 — la desactivation d'une regle laisse sa ponderation inchangee")
    void disablingARuleKeepsItsWeight() throws Exception {
        mockMvc.perform(put("/api/v1/rules/R2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("R2"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.weight").value(30));
    }

    @Test
    @DisplayName("UC6 — l'ajustement de la ponderation est pris en compte immediatement")
    void weightIsAdjustable() throws Exception {
        mockMvc.perform(put("/api/v1/rules/r7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight").value(12))
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(jsonPath("$[6].weight").value(12));
    }

    @Test
    @DisplayName("Un code de regle inconnu retourne 404 sans divulguer d'element interne")
    void unknownRuleIsRejected() throws Exception {
        mockMvc.perform(put("/api/v1/rules/R99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Regle inconnue"));
    }

    @Test
    @DisplayName("Une ponderation hors bornes est refusee")
    void outOfRangeWeightIsRejected() throws Exception {
        mockMvc.perform(put("/api/v1/rules/R1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", org.hamcrest.Matchers.hasSize(1)));
    }
}
