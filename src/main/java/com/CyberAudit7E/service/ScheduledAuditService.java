package com.cyberaudit7e.service;

import com.cyberaudit7e.domain.entity.Site;
import com.cyberaudit7e.dto.AuditRequestDto;
import com.cyberaudit7e.repository.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service d'audits programmés.
 *
 * M5 NOUVEAU : exécute automatiquement des audits périodiques
 * sur tous les sites enregistrés via @Scheduled.
 *
 * Configurable via application.yml :
 *   cyberaudit7e.scheduler.enabled: true/false
 *   cyberaudit7e.scheduler.cron: "0 0 2 * * *" (tous les jours à 2h)
 *
 * Analogie GitManager : équivalent des webhooks Gitea qui
 * déclenchent un audit automatique après chaque commit.
 * Ici c'est basé sur le temps (cron) plutôt que sur les événements.
 */
@Service
public class ScheduledAuditService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledAuditService.class);

    private final SiteRepository siteRepository;
    private final AsyncAuditService asyncAuditService;

    @Value("${cyberaudit7e.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    private LocalDateTime lastRunAt;
    private int lastRunCount;

    public ScheduledAuditService(SiteRepository siteRepository,
                                 AsyncAuditService asyncAuditService) {
        this.siteRepository = siteRepository;
        this.asyncAuditService = asyncAuditService;
    }

    /**
     * Audit périodique — exécuté selon le cron configuré.
     * Par défaut désactivé (cyberaudit7e.scheduler.enabled=false).
     *
     * @Scheduled cron : seconde minute heure jour mois jour-semaine
     * "0 0 2 * * *" = tous les jours à 02:00
     * "0 0/30 * * * *" = toutes les 30 minutes (pour le POC)
     */
    @Scheduled(cron = "${cyberaudit7e.scheduler.cron:0 0 2 * * *}")
    public void runScheduledAudits() {
        if (!schedulerEnabled) {
            return; // Silencieux si désactivé
        }

        List<Site> sites = siteRepository.findAll();

        if (sites.isEmpty()) {
            log.info("[SCHEDULER] Aucun site enregistré — rien à auditer");
            return;
        }

        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  AUDIT PROGRAMMÉ — {} site(s) à auditer", sites.size());
        log.info("╚══════════════════════════════════════════════════════╝");

        lastRunAt = LocalDateTime.now();
        lastRunCount = sites.size();

        // Lancer tous les audits en batch asynchrone
        List<AuditRequestDto> requests = sites.stream()
                .map(s -> new AuditRequestDto(s.getUrl(), s.getName()))
                .toList();

        asyncAuditService.submitBatch(requests);

        log.info("[SCHEDULER] {} audit(s) soumis en batch asynchrone", sites.size());
    }

    /**
     * Lancement manuel d'un audit programmé (via API).
     * Ignores le flag schedulerEnabled.
     */
    public int triggerNow() {
        List<Site> sites = siteRepository.findAll();
        if (sites.isEmpty()) return 0;

        log.info("[SCHEDULER] Déclenchement manuel — {} site(s)", sites.size());
        lastRunAt = LocalDateTime.now();
        lastRunCount = sites.size();

        List<AuditRequestDto> requests = sites.stream()
                .map(s -> new AuditRequestDto(s.getUrl(), s.getName()))
                .toList();

        asyncAuditService.submitBatch(requests);
        return sites.size();
    }

    /**
     * Informations sur le scheduler.
     */
    public SchedulerInfo getInfo() {
        return new SchedulerInfo(schedulerEnabled, lastRunAt, lastRunCount);
    }

    public record SchedulerInfo(
            boolean enabled,
            LocalDateTime lastRunAt,
            int lastRunSiteCount
    ) {}
}
