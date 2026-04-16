package com.cyberaudit7e.service.cycle;

import com.cyberaudit7e.domain.entity.AuditReport;
import com.cyberaudit7e.domain.enums.Phase7E;
import com.cyberaudit7e.event.AuditCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Phase 7E : ÉQUILIBRER
 * Boucle de rétroaction cybernétique — cœur du système.
 *
 * Écoute les AuditCompletedEvent et ajuste le comportement :
 * - Score faible → recommande d'augmenter les poids RGAA
 * - Tendance DOWN → alerte de régression
 * - Tendance UP → renforcement positif
 *
 * L'exécution est @Async pour ne pas bloquer le cycle principal.
 * C'est l'équivalent Spring du worker Celery d'AuditAccess.
 */
@Component
public class FeedbackLoopListener {

    private static final Logger log = LoggerFactory.getLogger(FeedbackLoopListener.class);

    private static final double SCORE_THRESHOLD_LOW = 0.5;
    private static final double SCORE_THRESHOLD_GOOD = 0.8;

    @Async
    @EventListener
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

        // Marquer le site comme ayant complété le cycle
        report.getSite().setCurrentPhase(Phase7E.EQUILIBRER);
        log.info("[7E-CYCLE COMPLET] {} → phase ÉQUILIBRER (cycle bouclé)",
                report.getSite().getName());
        log.info("══════════════════════════════════════════════════════");
    }
}
