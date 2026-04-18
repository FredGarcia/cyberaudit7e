package com.cyberaudit7e.service;

import com.cyberaudit7e.dto.AuditRequestDto;
import com.cyberaudit7e.dto.AuditResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service d'audit asynchrone.
 *
 * M5 NOUVEAU : permet de lancer des audits sans bloquer le thread HTTP.
 * Le client reçoit immédiatement un jobId et peut :
 *   - Suivre la progression via SSE (GET /api/audits/stream)
 *   - Consulter le résultat via GET /api/audits/async/{jobId}
 *
 * Supporte aussi les audits batch (plusieurs sites en parallèle).
 *
 * Analogie AuditAccess : équivalent du Celery task `analyze_repo`
 * lancé via Redis broker, mais sans infrastructure externe.
 */
@Service
public class AsyncAuditService {

    private static final Logger log = LoggerFactory.getLogger(AsyncAuditService.class);

    private final AuditOrchestrator orchestrator;

    /** Stockage des jobs async en cours et terminés */
    private final Map<Long, AsyncAuditJob> jobs = new ConcurrentHashMap<>();
    private final AtomicLong jobSequence = new AtomicLong(1);

    public AsyncAuditService(AuditOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Lance un audit asynchrone.
     * Retourne immédiatement un jobId.
     *
     * @param request DTO de la requête
     * @return Job avec ID et statut PENDING
     */
    public AsyncAuditJob submitAudit(AuditRequestDto request) {
        long jobId = jobSequence.getAndIncrement();
        AsyncAuditJob job = new AsyncAuditJob(jobId, request.url(), request.name());
        jobs.put(jobId, job);

        log.info("[ASYNC] Job #{} soumis pour {}", jobId, request.url());

        // Lancer l'exécution dans le pool @Async
        executeAsync(jobId, request);

        return job;
    }

    /**
     * Lance un batch d'audits en parallèle.
     * Chaque audit est un job indépendant.
     *
     * @param requests Liste des requêtes
     * @return Liste des jobs créés
     */
    public List<AsyncAuditJob> submitBatch(List<AuditRequestDto> requests) {
        log.info("[ASYNC-BATCH] Soumission de {} audit(s)", requests.size());
        return requests.stream()
                .map(this::submitAudit)
                .toList();
    }

    /**
     * Exécution asynchrone d'un audit via @Async.
     * Le thread HTTP est libéré immédiatement.
     */
    @Async("taskExecutor")
    public void executeAsync(long jobId, AuditRequestDto request) {
        AsyncAuditJob job = jobs.get(jobId);
        if (job == null) return;

        job.setStatus("RUNNING");
        log.info("[ASYNC] Job #{} démarré sur thread {}", jobId, Thread.currentThread().getName());

        try {
            AuditResponseDto result = orchestrator.executeFullCycle(request);
            job.setStatus("COMPLETED");
            job.setResult(result);
            log.info("[ASYNC] Job #{} terminé — score: {}", jobId, result.scores().get("global"));

        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setError(e.getMessage());
            log.error("[ASYNC] Job #{} échoué : {}", jobId, e.getMessage(), e);
        }
    }

    /**
     * Consulte le statut d'un job.
     */
    public AsyncAuditJob getJob(long jobId) {
        return jobs.get(jobId);
    }

    /**
     * Liste tous les jobs (actifs et terminés).
     */
    public List<AsyncAuditJob> getAllJobs() {
        return jobs.values().stream()
                .sorted((a, b) -> Long.compare(b.getJobId(), a.getJobId()))
                .toList();
    }

    /**
     * Nettoie les jobs terminés.
     */
    public int clearCompletedJobs() {
        List<Long> toRemove = jobs.entrySet().stream()
                .filter(e -> "COMPLETED".equals(e.getValue().getStatus())
                          || "FAILED".equals(e.getValue().getStatus()))
                .map(Map.Entry::getKey)
                .toList();
        toRemove.forEach(jobs::remove);
        log.info("[ASYNC] {} job(s) terminés nettoyés", toRemove.size());
        return toRemove.size();
    }

    // ── Inner class : AsyncAuditJob ──

    /**
     * Représentation d'un job d'audit asynchrone.
     */
    public static class AsyncAuditJob {
        private final long jobId;
        private final String siteUrl;
        private final String siteName;
        private final long submittedAt;
        private volatile String status;  // PENDING, RUNNING, COMPLETED, FAILED
        private volatile AuditResponseDto result;
        private volatile String error;

        public AsyncAuditJob(long jobId, String siteUrl, String siteName) {
            this.jobId = jobId;
            this.siteUrl = siteUrl;
            this.siteName = siteName;
            this.submittedAt = System.currentTimeMillis();
            this.status = "PENDING";
        }

        public long getJobId() { return jobId; }
        public String getSiteUrl() { return siteUrl; }
        public String getSiteName() { return siteName; }
        public long getSubmittedAt() { return submittedAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public AuditResponseDto getResult() { return result; }
        public void setResult(AuditResponseDto result) { this.result = result; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
