package com.cyberaudit7e.controller;

import com.cyberaudit7e.service.AuditEngine;
import com.cyberaudit7e.service.HtmlFetcherService;
import com.cyberaudit7e.service.ScoringService;
import com.cyberaudit7e.service.SseNotificationService;
import com.cyberaudit7e.service.cycle.AuditMetricsListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check et index API.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Santé", description = "Health check et découvrabilité de l'API")
public class HealthController {

    private final AuditEngine auditEngine;
    private final HtmlFetcherService htmlFetcher;
    private final ScoringService scoringService;
    private final SseNotificationService sseService;
    private final AuditMetricsListener metricsListener;

    public HealthController(AuditEngine auditEngine,
                            HtmlFetcherService htmlFetcher,
                            ScoringService scoringService,
                            SseNotificationService sseService,
                            AuditMetricsListener metricsListener) {
        this.auditEngine = auditEngine;
        this.htmlFetcher = htmlFetcher;
        this.scoringService = scoringService;
        this.sseService = sseService;
        this.metricsListener = metricsListener;
    }

    @Operation(
            summary = "Health check",
            description = "Retourne l'état du service, le nombre de règles chargées, les poids de scoring, les métriques et les clients SSE connectés."
    )
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "CyberAudit7E");
        response.put("version", "M6");
        response.put("phase", "7E-READY");
        response.put("rulesLoaded", auditEngine.getRulesCount());
        response.put("fetcherMode", "Jsoup (HTTP réel)");
        response.put("sseClients", sseService.getActiveClients());
        response.put("weights", scoringService.loadWeights());
        response.put("metrics", metricsListener.getMetrics());
        response.put("documentation", Map.of(
                "swagger-ui", "/swagger-ui.html",
                "openapi-json", "/v3/api-docs",
                "openapi-yaml", "/v3/api-docs.yaml"
        ));
        response.put("timestamp", Instant.now());
        return response;
    }

    @Operation(
            summary = "Index API",
            description = "Liste tous les endpoints disponibles avec leur description."
    )
    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("health", "GET /api/health — État du service");
        endpoints.put("sites", "GET /api/sites — Liste paginée des sites");
        endpoints.put("sites.create", "POST /api/sites — Enregistrer un site");
        endpoints.put("sites.search", "GET /api/sites/search?name=xxx — Recherche");
        endpoints.put("audits", "POST /api/audits — Audit synchrone (cycle 7E)");
        endpoints.put("audits.async", "POST /api/audits/async — Audit asynchrone");
        endpoints.put("audits.batch", "POST /api/audits/batch — Batch parallèle");
        endpoints.put("audits.stream", "GET /api/audits/stream — Flux SSE temps réel");
        endpoints.put("audits.list", "GET /api/audits/list — Rapports paginés");
        endpoints.put("audits.stats", "GET /api/audits/stats — Statistiques");
        endpoints.put("audits.alerts", "GET /api/audits/alerts — Alertes score faible");
        endpoints.put("scheduler", "GET /api/audits/schedule — Info scheduler");
        endpoints.put("scheduler.trigger", "POST /api/audits/schedule/trigger — Déclenchement manuel");
        endpoints.put("config.weights", "GET /api/config/weights — Poids de scoring");
        endpoints.put("swagger", "GET /swagger-ui.html — Documentation interactive");

        return Map.of(
                "service", "CyberAudit7E",
                "version", "M6 — API REST complète",
                "description", "Moteur d'audit cybernétique — Axiome 7E",
                "endpoints", endpoints
        );
    }
}
