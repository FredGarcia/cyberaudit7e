package com.cyberaudit7e.service;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.domain.rule.AuditContext;
import com.cyberaudit7e.domain.rule.AuditRule;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEngine M4 — Moteur Jsoup")
class AuditEngineTest {

    @Mock private ScoringService scoringService;
    @Mock private HtmlFetcherService htmlFetcher;

    private AuditEngine auditEngine;

    // Fausse règle pour les tests
    private final AuditRule fakeRule = new AuditRule() {
        @Override public String id() { return "TEST-01"; }
        @Override public String description() { return "Règle de test"; }
        @Override public RuleCategory category() { return RuleCategory.RGAA; }
        @Override public int priority() { return 50; }
        @Override
        public RuleResultDto evaluate(AuditContext context) {
            return context.hasDocument()
                    ? RuleResultDto.success(id(), category(), "DOM présent")
                    : RuleResultDto.failure(id(), category(), "Pas de DOM");
        }
    };

    @BeforeEach
    void setUp() {
        auditEngine = new AuditEngine(List.of(fakeRule), scoringService, htmlFetcher);
    }

    @Test
    @DisplayName("Exécute les règles avec un Document Jsoup réel")
    void shouldExecuteRulesWithDocument() {
        Document doc = Jsoup.parse("<html lang='fr'><head><title>Test</title></head><body></body></html>");
        when(htmlFetcher.fetch("https://test.fr")).thenReturn(Optional.of(doc));

        List<RuleResultDto> results = auditEngine.runAllRules("https://test.fr");

        assertEquals(1, results.size());
        assertTrue(results.getFirst().passed());
        assertEquals("DOM présent", results.getFirst().detail());
        verify(htmlFetcher).clearCache();
    }

    @Test
    @DisplayName("Mode dégradé si le crawl échoue")
    void shouldHandleFetchFailure() {
        when(htmlFetcher.fetch("https://down.invalid")).thenReturn(Optional.empty());

        List<RuleResultDto> results = auditEngine.runAllRules("https://down.invalid");

        assertEquals(1, results.size());
        assertFalse(results.getFirst().passed());
        assertEquals("Pas de DOM", results.getFirst().detail());
    }

    @Test
    @DisplayName("Attrape les exceptions des règles sans planter")
    void shouldCatchRuleExceptions() {
        AuditRule crashingRule = new AuditRule() {
            @Override public String id() { return "CRASH-01"; }
            @Override public String description() { return "Règle qui plante"; }
            @Override public RuleCategory category() { return RuleCategory.WCAG; }
            @Override
            public RuleResultDto evaluate(AuditContext context) {
                throw new RuntimeException("Boom");
            }
        };

        AuditEngine engine = new AuditEngine(
                List.of(crashingRule), scoringService, htmlFetcher);
        when(htmlFetcher.fetch(any())).thenReturn(Optional.empty());

        List<RuleResultDto> results = engine.runAllRules("https://test.fr");

        assertEquals(1, results.size());
        assertFalse(results.getFirst().passed());
        assertTrue(results.getFirst().detail().contains("Boom"));
    }

    @Test
    @DisplayName("Trie les règles par priorité")
    void shouldSortByPriority() {
        AuditRule lowPriority = new AuditRule() {
            @Override public String id() { return "LOW"; }
            @Override public String description() { return "Basse"; }
            @Override public RuleCategory category() { return RuleCategory.DSFR; }
            @Override public int priority() { return 200; }
            @Override public RuleResultDto evaluate(AuditContext c) {
                return RuleResultDto.success(id(), category(), "OK");
            }
        };
        AuditRule highPriority = new AuditRule() {
            @Override public String id() { return "HIGH"; }
            @Override public String description() { return "Haute"; }
            @Override public RuleCategory category() { return RuleCategory.RGAA; }
            @Override public int priority() { return 1; }
            @Override public RuleResultDto evaluate(AuditContext c) {
                return RuleResultDto.success(id(), category(), "OK");
            }
        };

        // Passer dans le désordre — le moteur doit trier
        AuditEngine engine = new AuditEngine(
                List.of(lowPriority, highPriority), scoringService, htmlFetcher);
        when(htmlFetcher.fetch(any())).thenReturn(Optional.empty());

        List<RuleResultDto> results = engine.runAllRules("https://test.fr");

        assertEquals("HIGH", results.get(0).ruleId());
        assertEquals("LOW", results.get(1).ruleId());
    }

    @Test
    @DisplayName("getRulesCount retourne le bon nombre")
    void shouldReturnRulesCount() {
        assertEquals(1, auditEngine.getRulesCount());
    }
}
