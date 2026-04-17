package com.cyberaudit7e.controller;

import com.cyberaudit7e.service.AuditEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NIVEAU 3 — Test de tranche Web (@WebMvcTest).
 *
 * Spring Boot 4 : import modularisé
 *   AVANT : org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
 *   APRÈS : org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
 *
 * Nécessite spring-boot-starter-webmvc-test dans le pom.xml (scope test).
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditEngine auditEngine;

    @Test
    @DisplayName("GET /api/health retourne 200 avec le bon statut")
    void healthEndpointReturnsUp() throws Exception {
        when(auditEngine.getRulesCount()).thenReturn(7);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("cyberaudit7e"))
                .andExpect(jsonPath("$.phase").value("7E-READY"))
                .andExpect(jsonPath("$.rulesLoaded").value(7))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
