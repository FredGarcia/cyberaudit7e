package com.cyberaudit7e.controller;

import com.cyberaudit7e.service.AuditEngine;
import com.cyberaudit7e.service.HtmlFetcherService;
import com.cyberaudit7e.service.ScoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController M4")
class HealthControllerTest {

    @Mock
    private AuditEngine auditEngine;
    @Mock
    private HtmlFetcherService htmlFetcher;
    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private HealthController controller;

    @Test
    @DisplayName("health() retourne UP avec les stats M4")
    void healthEndpointReturnsUp() {
        when(auditEngine.getRulesCount()).thenReturn(13);
        when(htmlFetcher.getCacheSize()).thenReturn(0);
        when(scoringService.loadWeights()).thenReturn(Map.of());

        Map<String, Object> result = controller.health();

        assertEquals("UP", result.get("status"));
        assertEquals("CyberAudit7E", result.get("service"));
        assertEquals("M4", result.get("version"));
        assertEquals(13, result.get("rulesLoaded"));
        assertEquals("Jsoup (HTTP réel)", result.get("fetcherMode"));
        assertEquals(0, result.get("fetcherCacheSize"));
        assertNotNull(result.get("timestamp"));
    }
}