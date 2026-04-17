package com.cyberaudit7e.service.cycle;

import com.cyberaudit7e.domain.entity.AuditReport;
import com.cyberaudit7e.domain.enums.Phase7E;
import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.event.AuditCompletedEvent;
import com.cyberaudit7e.repository.SiteRepository;
import com.cyberaudit7e.service.ScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 7E : ÉQUILIBRER — Boucle de rétroaction cybernétique.
 *
 * M4 : la rétroaction est réelle — le listener ajuste les poids
 * de scoring en BDD via ScoringService.updateWeight().
 *
 * Logique d'adaptation :
 * - Score RGAA faible → augmente le poids RGAA (+0.05) pour forcer la priorité
 * - Score DSFR faible sur un site .gouv.fr → augmente le poids DSFR
 * - Régression détectée → renforce la catégorie la plus faible
 *
 * C'est la cybernétique de 2e ordre : le système s'observe et se modifie.
 */
@Component
public class FeedbackLoopListener {

    private static final Logger log = LoggerFactory.getLogger(FeedbackLoopListener.class);

    private static final double SCORE_CRITICAL = 0.4;
    private static final double SCORE_LOW = 0.6;
    private static final double WEIGHT_ADJUSTMENT = 0.03;
    private static final double MAX_WEIGHT = 0.7;
    private static final double MIN_WEIGHT = 0.1;

    private final SiteRepository siteRepository;
    private final ScoringService scoringService;

    public FeedbackLoopListener(SiteRepository siteRepository,
                                ScoringService scoringService) {
        this.siteRepository = siteRepository;
        this.scoringService = scoringService;
    }

    @Async
    @EventListener
    @Transactional
    public void onAuditCompleted(AuditCompletedEvent event) {
        AuditReport report = event.getReport();
        String trend = event.getTrend();

        log.info("══════════════════════════════════════════════════════");
        log.info("[7E-ÉQUILIBRER] Analyse rétroactive du rapport #{}", report.getId());
        log.info("  ├─ Site    : {}", report.getSite().getName());
        log.info("  ├─ Score   : {}/1.00", report.getScoreGlobal());
        log.info("  ├─ RGAA    : {}", report.getScoreRgaa());
        log.info("  ├─ WCAG    : {}", report.getScoreWcag());
        log.info("  ├─ DSFR    : {}", report.getScoreDsfr());
        log.info("  └─ Tendance: {}", trend);

        // ── Logique de rétroaction adaptative ──

        boolean adjusted = false;

        // Règle 1 : Score RGAA critique → augmenter son poids
        if (report.getScoreRgaa() != null && report.getScoreRgaa() < SCORE_CRITICAL) {
            adjusted |= adjustWeight(RuleCategory.RGAA, WEIGHT_ADJUSTMENT,
                    "Score RGAA critique — renforcement");
        }

        // Règle 2 : Score WCAG faible → augmenter son poids
        if (report.getScoreWcag() != null && report.getScoreWcag() < SCORE_LOW) {
            adjusted |= adjustWeight(RuleCategory.WCAG, WEIGHT_ADJUSTMENT * 0.5,
                    "Score WCAG faible — ajustement léger");
        }

        // Règle 3 : Site .gouv.fr avec DSFR faible → renforcer DSFR
        if (report.getSite().getUrl().contains(".gouv.fr")
                && report.getScoreDsfr() != null
                && report.getScoreDsfr() < SCORE_LOW) {
            adjusted |= adjustWeight(RuleCategory.DSFR, WEIGHT_ADJUSTMENT,
                    "Site .gouv.fr avec DSFR faible — renforcement");
        }

        // Règle 4 : Régression détectée → renforcer la catégorie la plus faible
        if ("DOWN".equals(trend)) {
            RuleCategory weakest = findWeakestCategory(report);
            adjusted |= adjustWeight(weakest, WEIGHT_ADJUSTMENT,
                    String.format("Régression détectée — renforcement %s", weakest));
        }

        if (adjusted) {
            // Normaliser les poids pour qu'ils totalisent 1.0
            normalizeWeights();
        }

        if (!adjusted) {
            if ("UP".equals(trend)) {
                log.info("[7E-FEEDBACK] Amélioration confirmée — poids stables");
            } else {
                log.info("[7E-FEEDBACK] Scores dans la norme — aucun ajustement");
            }
        }

        // Mise à jour de la phase du site
        siteRepository.findById(report.getSite().getId()).ifPresent(site -> {
            site.setCurrentPhase(Phase7E.EQUILIBRER);
            siteRepository.save(site);
            log.info("[7E-CYCLE COMPLET] {} → phase ÉQUILIBRER (persisté en BDD)",
                    site.getName());
        });

        log.info("══════════════════════════════════════════════════════");
    }

    /**
     * Ajuste le poids d'une catégorie dans les limites [MIN_WEIGHT, MAX_WEIGHT].
     */
    private boolean adjustWeight(RuleCategory category, double delta, String reason) {
        var weights = scoringService.loadWeights();
        double current = weights.getOrDefault(category, category.getDefaultWeight());
        double newWeight = Math.min(MAX_WEIGHT, Math.max(MIN_WEIGHT, current + delta));

        if (Math.abs(newWeight - current) < 0.001) {
            return false; // Déjà au plafond/plancher
        }

        scoringService.updateWeight(category, newWeight);
        log.warn("[7E-FEEDBACK] {} — {} : {} → {}",
                reason, category, current, Math.round(newWeight * 1000.0) / 1000.0);
        return true;
    }

    /**
     * Normalise les poids pour qu'ils totalisent 1.0.
     */
    private void normalizeWeights() {
        var weights = scoringService.loadWeights();
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();

        if (Math.abs(total - 1.0) > 0.01) {
            log.info("[7E-FEEDBACK] Normalisation des poids (total actuel: {})", total);
            for (var entry : weights.entrySet()) {
                double normalized = Math.round(entry.getValue() / total * 1000.0) / 1000.0;
                scoringService.updateWeight(entry.getKey(), normalized);
            }
        }
    }

    /**
     * Identifie la catégorie avec le score le plus faible.
     */
    private RuleCategory findWeakestCategory(AuditReport report) {
        double rgaa = report.getScoreRgaa() != null ? report.getScoreRgaa() : 1.0;
        double wcag = report.getScoreWcag() != null ? report.getScoreWcag() : 1.0;
        double dsfr = report.getScoreDsfr() != null ? report.getScoreDsfr() : 1.0;

        if (rgaa <= wcag && rgaa <= dsfr) return RuleCategory.RGAA;
        if (wcag <= dsfr) return RuleCategory.WCAG;
        return RuleCategory.DSFR;
    }
}
