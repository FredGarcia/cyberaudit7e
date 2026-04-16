package com.cyberaudit7e.controller;

import com.cyberaudit7e.domain.entity.AuditReport;
import com.cyberaudit7e.dto.AuditRequestDto;
import com.cyberaudit7e.dto.AuditResponseDto;
import com.cyberaudit7e.repository.AuditReportRepository;
import com.cyberaudit7e.service.AuditOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller pour les audits.
 * Point d'entrée du cycle Axiome 7E.
 */
@RestController
@RequestMapping("/api/audits")
@Validated
public class AuditController {

    private final AuditOrchestrator orchestrator;
    private final AuditReportRepository reportRepository;

    public AuditController(AuditOrchestrator orchestrator,
                           AuditReportRepository reportRepository) {
        this.orchestrator = orchestrator;
        this.reportRepository = reportRepository;
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
    public ResponseEntity<AuditReport> getReport(@PathVariable Long id) {
        return reportRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/audits/site/{siteId} — Historique des audits d'un site.
     */
    @GetMapping("/site/{siteId}")
    public List<AuditReport> getSiteHistory(@PathVariable Long siteId) {
        return reportRepository.findBySiteIdOrderByAuditedAtDesc(siteId);
    }

    /**
     * GET /api/audits/stats — Statistiques globales du moteur.
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<AuditReport> all = reportRepository.findAll();
        double avgScore = all.stream()
                .mapToDouble(AuditReport::getScoreGlobal)
                .average()
                .orElse(0.0);

        return Map.of(
                "totalAudits", all.size(),
                "averageScore", Math.round(avgScore * 100.0) / 100.0
        );
    }
}
