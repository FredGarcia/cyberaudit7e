package com.cyberaudit7e.service;

import com.cyberaudit7e.domain.entity.AuditReport;
import com.cyberaudit7e.domain.entity.Site;
import com.cyberaudit7e.dto.AuditRequestDto;
import com.cyberaudit7e.dto.AuditResponseDto;
import com.cyberaudit7e.dto.RuleResultDto;
import com.cyberaudit7e.repository.AuditReportRepository;
import com.cyberaudit7e.repository.SiteRepository;
import com.cyberaudit7e.service.cycle.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Orchestrateur du cycle Axiome 7E.
 *
 * M3 : ajout de @Transactional pour garantir la cohérence
 * des écritures JPA (Site + AuditReport dans la même transaction).
 *
 * Si une phase échoue, toute la transaction est rollback.
 * C'est la garantie ACID que les repositories in-memory M2
 * ne pouvaient pas offrir.
 */
@Service
public class AuditOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AuditOrchestrator.class);

    private final SiteRepository siteRepository;
    private final AuditReportRepository reportRepository;
    private final EvaluateService evaluateService;
    private final ElaborateService elaborateService;
    private final ExecuteService executeService;
    private final ExamineService examineService;
    private final EvolveService evolveService;
    private final EmitService emitService;

    public AuditOrchestrator(
            SiteRepository siteRepository,
            AuditReportRepository reportRepository,
            EvaluateService evaluateService,
            ElaborateService elaborateService,
            ExecuteService executeService,
            ExamineService examineService,
            EvolveService evolveService,
            EmitService emitService
    ) {
        this.siteRepository = siteRepository;
        this.reportRepository = reportRepository;
        this.evaluateService = evaluateService;
        this.elaborateService = elaborateService;
        this.executeService = executeService;
        this.examineService = examineService;
        this.evolveService = evolveService;
        this.emitService = emitService;
    }

    /**
     * Exécute le cycle 7E complet dans une transaction unique.
     *
     * @Transactional garantit :
     * - Toutes les écritures (site + report) sont atomiques
     * - Un rollback si une phase lève une exception
     * - Le contexte de persistance JPA est actif pendant tout le cycle
     *   (pas de LazyInitializationException)
     */
    @Transactional
    public AuditResponseDto executeFullCycle(AuditRequestDto request) {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  CYCLE 7E — Démarrage audit pour {}", request.url());
        log.info("╚══════════════════════════════════════════════════════╝");

        // ── Résolution ou création du site ──
        Site site = siteRepository.findByUrl(request.url())
                .orElseGet(() -> {
                    log.info("[JPA] Création du site : {}", request.name());
                    Site newSite = new Site(request.url(), request.name());
                    return siteRepository.save(newSite);
                });

        // ── Phase 1 : ÉVALUER ──
        String validatedUrl = evaluateService.evaluate(site);

        // ── Phase 3 : EXÉCUTER (les règles du moteur) ──
        List<RuleResultDto> results = executeService.execute(validatedUrl);

        // ── Phase 2 : ÉLABORER (plan de remédiation) ──
        List<RuleResultDto> violations = elaborateService.elaborate(results);

        // ── Phase 4 : EXAMINER (scoring pondéré) ──
        Map<String, Double> scores = examineService.examine(results);

        // ── Phase 5 : ÉVOLUER (tendance) ──
        String trend = evolveService.evolve(site, scores);

        // ── Persistance du rapport (JPA) ──
        AuditReport report = new AuditReport(site, scores, results);
        report.setTrend(trend);
        report = reportRepository.save(report);

        // Bidirectionnel : mise à jour du côté Site
        site.addReport(report);
        siteRepository.save(site);

        log.info("[JPA] Rapport #{} persisté — score_global: {}", report.getId(), scores.get("global"));

        // ── Phase 6 : ÉMETTRE (événement — hors transaction) ──
        emitService.emit(report, trend);

        // ── Phase 7 : ÉQUILIBRER (asynchrone via FeedbackLoopListener) ──

        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  CYCLE 7E TERMINÉ — Rapport #{} — Score: {}",
                report.getId(), scores.get("global"));
        log.info("╚══════════════════════════════════════════════════════╝");

        return AuditResponseDto.from(
                report.getId(),
                site.getUrl(),
                site.getName(),
                scores,
                site.getCurrentPhase().name(),
                results,
                report.getAuditedAt()
        );
    }
}
