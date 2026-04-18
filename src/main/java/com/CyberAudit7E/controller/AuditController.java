package com.cyberaudit7e.controller;

import com.cyberaudit7e.dto.*;
import com.cyberaudit7e.repository.AuditReportRepository;
import com.cyberaudit7e.repository.SiteRepository;
import com.cyberaudit7e.service.AsyncAuditService;
import com.cyberaudit7e.service.AsyncAuditService.AsyncAuditJob;
import com.cyberaudit7e.service.AuditOrchestrator;
import com.cyberaudit7e.service.ScheduledAuditService;
import com.cyberaudit7e.service.SseNotificationService;
import com.cyberaudit7e.service.cycle.AuditMetricsListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller pour les audits.
 * M6 : documentation OpenAPI complète, pagination, réponses structurées.
 */
@RestController
@RequestMapping("/api/audits")
@Validated
public class AuditController {

    private final AuditOrchestrator orchestrator;
    private final AsyncAuditService asyncAuditService;
    private final ScheduledAuditService scheduledAuditService;
    private final SseNotificationService sseService;
    private final AuditMetricsListener metricsListener;
    private final AuditReportRepository reportRepository;
    private final SiteRepository siteRepository;

    public AuditController(AuditOrchestrator orchestrator,
                           AsyncAuditService asyncAuditService,
                           ScheduledAuditService scheduledAuditService,
                           SseNotificationService sseService,
                           AuditMetricsListener metricsListener,
                           AuditReportRepository reportRepository,
                           SiteRepository siteRepository) {
        this.orchestrator = orchestrator;
        this.asyncAuditService = asyncAuditService;
        this.scheduledAuditService = scheduledAuditService;
        this.sseService = sseService;
        this.metricsListener = metricsListener;
        this.reportRepository = reportRepository;
        this.siteRepository = siteRepository;
    }

    // ═══════════════════════════════════════════
    // AUDIT SYNCHRONE
    // ═══════════════════════════════════════════

    @Tag(name = "Audits")
    @Operation(
            summary = "Lancer un audit synchrone",
            description = "Exécute le cycle 7E complet (Évaluer→Équilibrer) de façon bloquante. " +
                    "Le crawl HTTP et les 13 règles sont exécutés avant de retourner le résultat."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Audit terminé — rapport créé"),
            @ApiResponse(responseCode = "400", description = "Requête invalide (URL ou nom manquant)")
    })
    @PostMapping
    public ResponseEntity<AuditResponseDto> runAudit(
            @Valid @RequestBody AuditRequestDto request) {
        AuditResponseDto result = orchestrator.executeFullCycle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ═══════════════════════════════════════════
    // CONSULTATION & PAGINATION (M6)
    // ═══════════════════════════════════════════

    @Tag(name = "Audits")
    @Operation(
            summary = "Lister les rapports d'audit (paginé)",
            description = "Retourne les rapports d'audit paginés et triés. " +
                    "Paramètres de pagination : page (0-based), size, sort (champ,direction)."
    )
    @GetMapping("/list")
    @Transactional(readOnly = true)
    public PagedResponse<ReportSummaryDto> listReports(
            @Parameter(description = "Numéro de page (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri", example = "auditedAt")
            @RequestParam(defaultValue = "auditedAt") String sortBy,
            @Parameter(description = "Direction du tri", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        return PagedResponse.from(
                reportRepository.findAll(pageable).map(ReportSummaryDto::from));
    }

    @Tag(name = "Audits")
    @Operation(
            summary = "Consulter un rapport d'audit",
            description = "Retourne le détail complet d'un rapport incluant les scores par catégorie et le résultat de chaque règle."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rapport trouvé"),
            @ApiResponse(responseCode = "404", description = "Rapport introuvable")
    })
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ReportSummaryDto> getReport(
            @Parameter(description = "ID du rapport", example = "1")
            @PathVariable Long id) {
        return reportRepository.findById(id)
                .map(ReportSummaryDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Tag(name = "Audits")
    @Operation(
            summary = "Historique des audits d'un site (paginé)",
            description = "Retourne l'historique paginé des audits pour un site donné, trié par date décroissante."
    )
    @GetMapping("/site/{siteId}")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponse<ReportSummaryDto>> getSiteHistory(
            @Parameter(description = "ID du site", example = "1")
            @PathVariable Long siteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (!siteRepository.existsById(siteId)) {
            return ResponseEntity.notFound().build();
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("auditedAt").descending());

        return ResponseEntity.ok(PagedResponse.from(
                reportRepository.findBySiteId(siteId, pageable).map(ReportSummaryDto::from)));
    }

    @Tag(name = "Audits")
    @Operation(
            summary = "Rechercher des rapports",
            description = "Recherche full-text par nom ou URL de site dans les rapports d'audit."
    )
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public PagedResponse<ReportSummaryDto> searchReports(
            @Parameter(description = "Terme de recherche", example = "gouv")
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("auditedAt").descending());
        return PagedResponse.from(
                reportRepository.searchByQuery(q, pageable).map(ReportSummaryDto::from));
    }

    @Tag(name = "Audits")
    @Operation(
            summary = "Alertes — rapports sous le seuil",
            description = "Retourne les rapports dont le score global est inférieur au seuil spécifié."
    )
    @GetMapping("/alerts")
    @Transactional(readOnly = true)
    public PagedResponse<ReportSummaryDto> getAlerts(
            @Parameter(description = "Seuil de score (0.0 à 1.0)", example = "0.5")
            @RequestParam(defaultValue = "0.5") Double threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("scoreGlobal").ascending());
        return PagedResponse.from(
                reportRepository.findByScoreGlobalLessThan(threshold, pageable)
                        .map(ReportSummaryDto::from));
    }

    // ═══════════════════════════════════════════
    // ASYNC & BATCH (M5 — conservés avec doc M6)
    // ═══════════════════════════════════════════

    @Tag(name = "Audits Async")
    @Operation(
            summary = "Lancer un audit asynchrone",
            description = "Soumet un audit non-bloquant. Retourne immédiatement un jobId. " +
                    "Suivre la progression via SSE (GET /api/audits/stream) ou polling (GET /api/audits/async/{jobId})."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Audit soumis — jobId retourné"),
            @ApiResponse(responseCode = "400", description = "Requête invalide")
    })
    @PostMapping("/async")
    public ResponseEntity<Map<String, Object>> runAsyncAudit(
            @Valid @RequestBody AuditRequestDto request) {
        AsyncAuditJob job = asyncAuditService.submitAudit(request);
        return ResponseEntity.accepted().body(Map.of(
                "jobId", job.getJobId(),
                "status", job.getStatus(),
                "siteUrl", job.getSiteUrl(),
                "message", "Audit soumis — suivez via SSE ou GET /api/audits/async/" + job.getJobId(),
                "sseEndpoint", "/api/audits/stream"
        ));
    }

    @Tag(name = "Audits Async")
    @Operation(
            summary = "Lancer un batch d'audits",
            description = "Soumet plusieurs audits en parallèle (max 10). " +
                    "Chaque audit est un job indépendant exécuté sur le pool de threads."
    )
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> runBatchAudit(
            @Valid @RequestBody BatchAuditRequestDto request) {
        List<AsyncAuditJob> jobs = asyncAuditService.submitBatch(request.sites());
        List<Map<String, Object>> jobSummaries = jobs.stream()
                .map(j -> Map.<String, Object>of(
                        "jobId", j.getJobId(), "siteUrl", j.getSiteUrl(), "status", j.getStatus()))
                .toList();
        return ResponseEntity.accepted().body(Map.of(
                "batchSize", jobs.size(), "jobs", jobSummaries,
                "message", "Batch soumis — suivez via SSE"));
    }

    @Tag(name = "Audits Async")
    @Operation(summary = "Statut d'un job asynchrone")
    @GetMapping("/async/{jobId}")
    public ResponseEntity<?> getAsyncJob(
            @Parameter(description = "ID du job", example = "1")
            @PathVariable Long jobId) {
        AsyncAuditJob job = asyncAuditService.getJob(jobId);
        if (job == null) return ResponseEntity.notFound().build();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", job.getJobId());
        response.put("siteUrl", job.getSiteUrl());
        response.put("siteName", job.getSiteName());
        response.put("status", job.getStatus());
        if ("COMPLETED".equals(job.getStatus()) && job.getResult() != null)
            response.put("result", job.getResult());
        if ("FAILED".equals(job.getStatus()) && job.getError() != null)
            response.put("error", job.getError());
        return ResponseEntity.ok(response);
    }

    @Tag(name = "Audits Async")
    @Operation(summary = "Lister tous les jobs asynchrones")
    @GetMapping("/async")
    public List<Map<String, Object>> listAsyncJobs() {
        return asyncAuditService.getAllJobs().stream()
                .map(j -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("jobId", j.getJobId());
                    m.put("siteUrl", j.getSiteUrl());
                    m.put("status", j.getStatus());
                    if ("COMPLETED".equals(j.getStatus()) && j.getResult() != null)
                        m.put("scoreGlobal", j.getResult().scores().get("global"));
                    return m;
                }).toList();
    }

    @Tag(name = "Audits Async")
    @Operation(summary = "Nettoyer les jobs terminés")
    @DeleteMapping("/async")
    public Map<String, Object> clearJobs() {
        int cleared = asyncAuditService.clearCompletedJobs();
        return Map.of("cleared", cleared, "message", cleared + " job(s) nettoyé(s)");
    }

    // ═══════════════════════════════════════════
    // SSE STREAMING
    // ═══════════════════════════════════════════

    @Tag(name = "Audits Async")
    @Operation(
            summary = "Flux SSE temps réel",
            description = """
                    Ouvre une connexion Server-Sent Events pour recevoir les événements d'audit en temps réel.

                    **Événements émis** :
                    - `connected` : confirmation de connexion
                    - `audit-started` : début d'un audit (site URL + nom)
                    - `audit-progress` : transition de phase 7E (×7, avec pourcentage)
                    - `audit-completed` : fin de l'audit (score + tendance)

                    **Utilisation curl** : `curl -N http://localhost:8080/api/audits/stream`

                    **Utilisation JavaScript** :
                    ```javascript
                    const sse = new EventSource('/api/audits/stream');
                    sse.addEventListener('audit-progress', (e) => {
                      const data = JSON.parse(e.data);
                      console.log(`${data.phaseLabel} — ${data.progress}%`);
                    });
                    ```
                    """
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAudits() {
        return sseService.subscribe();
    }

    // ═══════════════════════════════════════════
    // SCHEDULER
    // ═══════════════════════════════════════════

    @Tag(name = "Scheduler")
    @Operation(
            summary = "Déclencher un audit de tous les sites",
            description = "Lance immédiatement un batch asynchrone sur tous les sites enregistrés, " +
                    "indépendamment de la configuration du scheduler."
    )
    @PostMapping("/schedule/trigger")
    public Map<String, Object> triggerScheduledAudit() {
        int count = scheduledAuditService.triggerNow();
        return Map.of("triggered", count,
                "message", count > 0 ? count + " audit(s) lancé(s)" : "Aucun site enregistré");
    }

    @Tag(name = "Scheduler")
    @Operation(summary = "Informations sur le scheduler")
    @GetMapping("/schedule")
    public ScheduledAuditService.SchedulerInfo getSchedulerInfo() {
        return scheduledAuditService.getInfo();
    }

    // ═══════════════════════════════════════════
    // STATISTIQUES
    // ═══════════════════════════════════════════

    @Tag(name = "Audits")
    @Operation(
            summary = "Statistiques globales",
            description = "Retourne les statistiques agrégées : nombre de sites/audits, score moyen, " +
                    "métriques de performance, répartition par tendance et par phase."
    )
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSites", siteRepository.count());
        stats.put("totalAudits", reportRepository.count());
        stats.put("averageScore", reportRepository.averageGlobalScore());

        stats.put("metrics", metricsListener.getMetrics());
        stats.put("sseClients", sseService.getActiveClients());
        stats.put("scheduler", scheduledAuditService.getInfo());

        Map<String, Long> trendCounts = new LinkedHashMap<>();
        for (String trend : List.of("FIRST", "UP", "DOWN", "STABLE")) {
            trendCounts.put(trend, (long) reportRepository.findByTrend(trend).size());
        }
        stats.put("trends", trendCounts);

        Map<String, Long> phaseCounts = new LinkedHashMap<>();
        siteRepository.countByPhase().forEach(row ->
                phaseCounts.put(row[0].toString(), (Long) row[1]));
        stats.put("sitesByPhase", phaseCounts);

        return stats;
    }
}
