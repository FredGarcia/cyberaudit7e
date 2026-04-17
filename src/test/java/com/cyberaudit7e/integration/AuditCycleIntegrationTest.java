package com.cyberaudit7e.integration;

import com.cyberaudit7e.domain.entity.AuditReport;
import com.cyberaudit7e.dto.AuditRequestDto;
import com.cyberaudit7e.dto.AuditResponseDto;
import com.cyberaudit7e.repository.AuditReportRepository;
import com.cyberaudit7e.service.AuditOrchestrator;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NIVEAU 4 — Test d'intégration complet (@SpringBootTest).
 *
 * Démarre le contexte Spring COMPLET :
 *   - Toutes les configs
 *   - Toutes les beans (controllers, services, repos, config, events)
 *   - Flyway applique le schéma + seed data
 *   - H2 in-memory
 *
 * C'est le test le plus lent mais le plus représentatif.
 * On l'utilise pour valider le cycle 7E de bout en bout.
 *
 * Awaitility permet de tester les comportements asynchrones
 * (FeedbackLoopListener @Async).
 */
@SpringBootTest
@ActiveProfiles("dev")
class AuditCycleIntegrationTest {

    @Autowired
    private AuditOrchestrator orchestrator;

    @Autowired
    private AuditReportRepository reportRepository;

    @Test
    @DisplayName("Cycle 7E complet sur un site .gouv.fr produit un score DSFR élevé")
    void fullCycleOnGouvFrSite() {
        AuditRequestDto request = new AuditRequestDto(
                "https://www.test-integration.gouv.fr",
                "Test Integration"
        );

        AuditResponseDto response = orchestrator.executeFullCycle(request);

        assertThat(response.reportId()).isNotNull();
        assertThat(response.siteUrl()).isEqualTo(request.url());
        assertThat(response.scores()).containsKeys("rgaa", "wcag", "dsfr", "global");
        assertThat(response.scores().get("dsfr")).isGreaterThan(0.8); // .gouv.fr → DSFR élevé
        assertThat(response.rulesCount()).isEqualTo(7);
        assertThat(response.details()).hasSize(7);
    }

    @Test
    @DisplayName("Cycle 7E sur site non-.gouv.fr → score DSFR faible")
    void fullCycleOnNonGouvSite() {
        AuditRequestDto request = new AuditRequestDto(
                "https://www.example.com",
                "Example"
        );

        AuditResponseDto response = orchestrator.executeFullCycle(request);

        assertThat(response.scores().get("dsfr")).isLessThan(0.2);
    }

    @Test
    @DisplayName("Deuxième audit d'un site → tendance STABLE détectée")
    void trendStableOnRepeatedAudit() {
        AuditRequestDto request = new AuditRequestDto(
                "https://www.trend-test.gouv.fr",
                "Trend Test"
        );

        // Premier audit → FIRST
        AuditResponseDto first = orchestrator.executeFullCycle(request);

        // Deuxième audit → STABLE (scores déterministes sur la même URL)
        AuditResponseDto second = orchestrator.executeFullCycle(request);

        // Vérifier que le 2e rapport a bien la tendance "STABLE" en BDD
        Awaitility.await()
                .atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    AuditReport report = reportRepository.findById(second.reportId()).orElseThrow();
                    assertThat(report.getTrend()).isEqualTo("STABLE");
                });
    }

    @Test
    @DisplayName("Le rapport est persisté en BDD avec les détails JSON")
    void reportIsPersistedWithJsonDetails() {
        AuditRequestDto request = new AuditRequestDto(
                "https://www.persist-test.gouv.fr",
                "Persist Test"
        );

        AuditResponseDto response = orchestrator.executeFullCycle(request);

        AuditReport persisted = reportRepository.findById(response.reportId()).orElseThrow();
        assertThat(persisted.getScoreGlobal()).isEqualTo(response.scores().get("global"));
        assertThat(persisted.getRuleResults()).hasSize(7); // converter JSON → List
        assertThat(persisted.getAuditedAt()).isNotNull();
    }
}
