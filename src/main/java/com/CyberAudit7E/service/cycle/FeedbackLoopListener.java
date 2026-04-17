package com.cyberaudit7e.service.cycle;

import com.cyberaudit7e.domain.entity.AuditReport;
import com.cyberaudit7e.domain.enums.Phase7E;
import com.cyberaudit7e.event.AuditCompletedEvent;
import com.cyberaudit7e.repository.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 7E : ÉQUILIBRER — Boucle de rétroaction cybernétique.
 *
 * M3 : ajout de @Transactional car la mise à jour de la phase du site
 * doit être persistée via JPA. En M2, le POJO in-memory était
 * directement modifiable ; avec JPA, il faut une transaction.
 */
@Component
public class FeedbackLoopListener {

    private static final Logger log = LoggerFactory.getLogger(FeedbackLoopListener.class);

    private static final double SCORE_THRESHOLD_LOW = 0.5;
    private static final double SCORE_THRESHOLD_GOOD = 0.8;

    private final SiteRepository siteRepository;

    public FeedbackLoopListener(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Async
    @EventListener
    @Transactional
    public void onAuditCompleted(AuditCompletedEvent event) {
        AuditReport report = event.getReport();
        String trend = event.getTrend();

        log.info("══════════════════════════════════════════════════════");
        log.info("[7E-ÉQUILIBRER] Analyse rétroactive du rapport #{}",
                report.getId());
        log.info("  ├─ Site    : {}", report.getSite().getName());
        log.info("  ├─ Score   : {}/1.00", report.getScoreGlobal());
        log.info("  ├─ RGAA    : {}", report.getScoreRgaa());
        log.info("  ├─ WCAG    : {}", report.getScoreWcag());
        log.info("  ├─ DSFR    : {}", report.getScoreDsfr());
        log.info("  └─ Tendance: {}", trend);

        // ── Logique de rétroaction ──
        if (report.getScoreGlobal() < SCORE_THRESHOLD_LOW) {
            log.warn("[7E-FEEDBACK] Score critique ({}) — Action : " +
                     "augmenter la pondération RGAA, prioriser les corrections",
                    report.getScoreGlobal());
        } else if (report.getScoreGlobal() >= SCORE_THRESHOLD_GOOD) {
            log.info("[7E-FEEDBACK] Score excellent ({}) — " +
                     "le site est en bonne voie de conformité",
                    report.getScoreGlobal());
        }

        if ("DOWN".equals(trend)) {
            log.warn("[7E-FEEDBACK] Régression détectée ! " +
                     "Vérifier les dernières modifications du site.");
        } else if ("UP".equals(trend)) {
            log.info("[7E-FEEDBACK] Amélioration confirmée — " +
                     "les corrections portent leurs fruits.");
        }

        // ── Mise à jour JPA de la phase du site ──
        siteRepository.findById(report.getSite().getId()).ifPresent(site -> {
            site.setCurrentPhase(Phase7E.EQUILIBRER);
            siteRepository.save(site);
            log.info("[7E-CYCLE COMPLET] {} → phase ÉQUILIBRER (persisté en BDD)",
                    site.getName());
        });

        log.info("══════════════════════════════════════════════════════");
    }
}
