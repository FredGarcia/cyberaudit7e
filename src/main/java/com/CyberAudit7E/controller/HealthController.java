package com.cyberaudit7e.controller;

import com.cyberaudit7e.service.AuditEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check endpoint.
 * M1 : version basique.
 * M2 : enrichi avec les stats du moteur (nombre de règles chargées).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final AuditEngine auditEngine;

    public HealthController(AuditEngine auditEngine) {
        this.auditEngine = auditEngine;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "CyberAudit7E",
                "phase", "7E-READY",
                "rulesLoaded", auditEngine.getRulesCount(),
                "timestamp", Instant.now()
        );
    }
}
