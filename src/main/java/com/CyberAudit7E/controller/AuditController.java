package com.CyberAudit7E.controller;

import com.CyberAudit7E.dto.AuditRequestDto;
import com.CyberAudit7E.dto.AuditResponseDto;
import com.CyberAudit7E.dto.ReportSummaryDto;
import com.CyberAudit7E.repository.AuditReportRepository;
import com.CyberAudit7E.repository.SiteRepository;
import com.CyberAudit7E.service.AuditOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller pour les audits.
 *
 * M3 : retourne des DTOs (ReportSummaryDto) au lieu d'entités JPA.
 * Les endpoints utilisent @Transactional(readOnly=true) pour les lectures.
 */
@RestController
@RequestMapping("/api/audits")
@Validated
public class AuditController {

    private final AuditOrchestrator orchestrator;
    private final AuditReportRepository reportRepository;
    private final SiteRepository siteRepository;

    public AuditController(AuditOrchestrator orchestrator,
            AuditReportRepository reportRepository,
            SiteRepository siteRepository) {
        this.orchestrator = orchestrator;
        this.reportRepository = reportRepository;
        this.siteRepository = siteRepository;
    }

    /**
     * POST /api/audits — Lancer un audit complet (cycle 7E).
     */
    @PostMapping
    public ResponseEntity<AuditResponseDto> runAudit(
            @Valid @RequestBody AuditRequestDto request) {
        AuditResponseDto result = orchestrator.executeFullCycle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /api/audits/{id} — Consulter un rapport d'audit.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ReportSummaryDto> getReport(@PathVariable Long id) {
        return reportRepository.findById(id)
                .map(ReportSummaryDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/audits/site/{siteId} — Historique des audits d'un site.
     */
    @GetMapping("/site/{siteId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSiteHistory(@PathVariable Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            return ResponseEntity.notFound().build();
        }
        List<ReportSummaryDto> history = reportRepository
                .findBySiteIdOrderByAuditedAtDesc(siteId)
                .stream()
                .map(ReportSummaryDto::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/audits/alerts — Rapports avec score sous le seuil (0.5).
     */
    @GetMapping("/alerts")
    @Transactional(readOnly = true)
    public List<ReportSummaryDto> getAlerts(
            @RequestParam(defaultValue = "0.5") Double threshold) {
        return reportRepository.findByScoreGlobalLessThan(threshold)
                .stream()
                .map(ReportSummaryDto::from)
                .toList();
    }

    /**
     * GET /api/audits/stats — Statistiques globales du moteur.
     * Utilise les Query Methods et JPQL custom de M3.
     */
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSites", siteRepository.count());
        stats.put("totalAudits", reportRepository.count());
        stats.put("averageScore", reportRepository.averageGlobalScore());

        // Répartition par tendance
        Map<String, Long> trendCounts = new LinkedHashMap<>();
        for (String trend : List.of("FIRST", "UP", "DOWN", "STABLE")) {
            trendCounts.put(trend, (long) reportRepository.findByTrend(trend).size());
        }
        stats.put("trends", trendCounts);

        // Répartition par phase
        Map<String, Long> phaseCounts = new LinkedHashMap<>();
        siteRepository.countByPhase().forEach(row -> phaseCounts.put(row[0].toString(), (Long) row[1]));
        stats.put("sitesByPhase", phaseCounts);

        return stats;
    }
}
