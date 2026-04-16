package com.CyberAudit7E.domain.rule;

import com.CyberAudit7E.domain.enums.RuleCategory;
import com.CyberAudit7E.dto.RuleResultDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NIVEAU 1 — Test unitaire d'une règle (Strategy Pattern).
 *
 * Démontre @Nested pour organiser les tests par scénario.
 */
class DsfrHeaderRuleTest {

    private final DsfrHeaderRule rule = new DsfrHeaderRule();

    @Test
    @DisplayName("Métadonnées de la règle")
    void ruleMetadata() {
        assertThat(rule.id()).isEqualTo("DSFR-HDR-01");
        assertThat(rule.category()).isEqualTo(RuleCategory.DSFR);
        assertThat(rule.description()).isNotBlank();
    }

    @Nested
    @DisplayName("Sur un site .gouv.fr")
    class GouvFrSite {

        @Test
        @DisplayName("Site .gouv.fr → règle passe avec score 1.0")
        void gouvFrPasses() {
            RuleResultDto result = rule.evaluate("https://www.gouvernement.gouv.fr");

            assertThat(result.passed()).isTrue();
            assertThat(result.score()).isEqualTo(1.0);
            assertThat(result.ruleId()).isEqualTo("DSFR-HDR-01");
        }
    }

    @Nested
    @DisplayName("Sur un site non-.gouv.fr")
    class NonGouvFrSite {

        @Test
        @DisplayName("Site .com → règle échoue avec score 0")
        void nonGouvFrFails() {
            RuleResultDto result = rule.evaluate("https://www.example.com");

            assertThat(result.passed()).isFalse();
            assertThat(result.score()).isZero();
        }

        @Test
        @DisplayName("Site .fr (non-.gouv) → règle échoue")
        void nonGouvFrDotFrFails() {
            RuleResultDto result = rule.evaluate("https://www.service-public.fr");

            assertThat(result.passed()).isFalse();
        }
    }
}
