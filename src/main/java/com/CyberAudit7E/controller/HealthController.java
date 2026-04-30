package com.cyberaudit7e.controller;

import com.cyberaudit7e.service.AuditEngine;
import com.cyberaudit7e.service.HtmlFetcherService;
import com.cyberaudit7e.service.ScoringService;
import com.cyberaudit7e.service.SseNotificationService;
import com.cyberaudit7e.service.cycle.AuditMetricsListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check et index API.
 * M7 : enrichi avec les infos runtime/container pour le monitoring Docker.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Santé", description = "Health check, métriques runtime et découvrabilité API")
public class HealthController {

    private final AuditEngine auditEngine;
    private final HtmlFetcherService htmlFetcher;
    private final ScoringService scoringService;
    private final SseNotificationService sseService;
    private final AuditMetricsListener metricsListener;
    private final Instant startedAt = Instant.now();

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

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
            summary = "Health check complet",
            description = "Retourne l'état du service, les métriques runtime (mémoire, uptime), " +
                    "le nombre de règles, les poids de scoring et les clients SSE."
    )
    @GetMapping("/health")
    public Map<String, Object> health() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        Duration uptime = Duration.between(startedAt, Instant.now());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "CyberAudit7E");
        response.put("version", "M7");
        response.put("profile", activeProfile);
        response.put("phase", "7E-READY");
        response.put("rulesLoaded", auditEngine.getRulesCount());
        response.put("fetcherMode", "Jsoup (HTTP réel)");
        response.put("sseClients", sseService.getActiveClients());
        response.put("weights", scoringService.loadWeights());
        response.put("metrics", metricsListener.getMetrics());

        // Runtime / container info
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("javaVersion", System.getProperty("java.version"));
        runtime.put("heapUsedMb", heapUsed);
        runtime.put("heapMaxMb", heapMax);
        runtime.put("heapPercent", heapMax > 0 ? Math.round((double) heapUsed / heapMax * 100) : 0);
        runtime.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        runtime.put("uptimeSeconds", uptime.getSeconds());
        runtime.put("uptime", formatDuration(uptime));
        runtime.put("hostname", getHostname());
        response.put("runtime", runtime);

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
            description = "Liste tous les endpoints disponibles avec descriptions."
    )
    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("health", "GET /api/health");
        endpoints.put("swagger", "GET /swagger-ui.html");
        endpoints.put("sites", "GET /api/sites — CRUD paginé");
        endpoints.put("audits.sync", "POST /api/audits — Cycle 7E synchrone");
        endpoints.put("audits.async", "POST /api/audits/async — Non-bloquant");
        endpoints.put("audits.batch", "POST /api/audits/batch — Parallèle (max 10)");
        endpoints.put("audits.stream", "GET /api/audits/stream — SSE temps réel");
        endpoints.put("audits.list", "GET /api/audits/list — Rapports paginés");
        endpoints.put("audits.search", "GET /api/audits/search?q=xxx — Recherche");
        endpoints.put("audits.stats", "GET /api/audits/stats — Statistiques");
        endpoints.put("audits.alerts", "GET /api/audits/alerts — Score faible");
        endpoints.put("scheduler", "POST /api/audits/schedule/trigger — Déclenchement");
        endpoints.put("config", "GET /api/config/weights — Poids dynamiques");

        return Map.of(
                "service", "CyberAudit7E",
                "version", "M7 — Docker & Synthèse",
                "axiome", "Les Éléments dans l'Espace Engendrent un État d'Expression Évolutif de l'Environnement",
                "endpoints", endpoints
        );
    }

    private String formatDuration(Duration d) {
        long h = d.toHours();
        long m = d.toMinutesPart();
        long s = d.toSecondsPart();
        if (h > 0) return String.format("%dh%02dm%02ds", h, m, s);
        if (m > 0) return String.format("%dm%02ds", m, s);
        return String.format("%ds", s);
    }

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return System.getenv().getOrDefault("HOSTNAME", "unknown");
        }
    }
}
