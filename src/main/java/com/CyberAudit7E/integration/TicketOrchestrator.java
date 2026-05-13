package com.cyberaudit7e.integration;

import com.cyberaudit7e.domain.entity.SecurityTicket;
/* import com.cyberaudit7e.domain.entity.SystemSetting; */
import com.cyberaudit7e.domain.enums.TicketSeverity;
import com.cyberaudit7e.domain.enums.TicketSource;
import com.cyberaudit7e.domain.enums.TicketStatus;
import com.cyberaudit7e.integration.servicenow.ServiceNowClient;
import com.cyberaudit7e.repository.SecurityTicketRepository;
import com.cyberaudit7e.repository.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Orchestrateur de tickets — le pont central entre les trois systèmes.
 *
 * Responsabilités :
 * 1. Persister les SecurityTickets en BDD
 * 2. Pousser les tickets vers ServiceNow (création d'incidents)
 * 3. Dédoublonner les violations SailPoint (ne pas créer 2 tickets pour la même erreur)
 * 4. Mettre à jour les tickets quand ServiceNow ou SailPoint notifie un changement
 * 5. Créer des tickets depuis les audits CyberAudit7E (score critique)
 *
 * Flux typique :
 * SailPoint Event → processIncomingTicket() → save + pushToServiceNow()
 * Audit complet → createFromAudit() → save + pushToServiceNow()
 * SNOW callback → (géré par ServiceNowWebhookController)
 *
 * le seuil de creation automatique est lu depuis la table
 * system_settings (cle "ticket.auto.threshold") au lieu d'etre code en dur.
 * Modifiable via PUT /api/config/settings/ticket.auto.threshold
 */

@Service
public class TicketOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TicketOrchestrator.class);

    /** Cle du seuil dans system_settings */
    private static final String THRESHOLD_KEY = "ticket.auto.threshold";
    /** Valeur par defaut si la cle n'existe pas en BDD */
    private static final double DEFAULT_THRESHOLD = 0.5;

    private final SecurityTicketRepository ticketRepository;
    private final SystemSettingRepository settingRepository;
    private final ServiceNowClient serviceNowClient;

    public TicketOrchestrator(SecurityTicketRepository ticketRepository,
            SystemSettingRepository settingRepository,
            ServiceNowClient serviceNowClient) {
        this.ticketRepository = ticketRepository;
        this.settingRepository = settingRepository;
        this.serviceNowClient = serviceNowClient;
    }

    /**
     * Lit le seuil de creation automatique depuis la BDD.
     * Fallback sur DEFAULT_THRESHOLD (0.5) si absent.
     */
    public double getAutoTicketThreshold() {
        return settingRepository.findById(THRESHOLD_KEY)
                .map(s -> s.asDouble(DEFAULT_THRESHOLD))
                .orElse(DEFAULT_THRESHOLD);
    }

    /**
     * Traite un ticket entrant (SailPoint webhook ou création manuelle).
     *
     * 1. Vérifie le dédoublonnage (même violation SailPoint ?)
     * 2. Persiste le ticket
     * 3. Pousse vers ServiceNow de façon asynchrone
     *
     * @param ticket Le ticket à traiter
     * @return Le ticket persisté
     */
    @Transactional
    public SecurityTicket processIncomingTicket(SecurityTicket ticket) {
        log.info("[ORCHESTRATOR] Traitement ticket — source: {}, severite: {}",
                ticket.getSource(), ticket.getSeverity());

        // Dedoublonnage SailPoint
        if (ticket.getSource() == TicketSource.SAILPOINT
                && ticket.getSailpointViolationId() != null) {

            Optional<SecurityTicket> existing = ticketRepository
                    .findBySailpointViolationId(ticket.getSailpointViolationId());

            if (existing.isPresent()) {
                SecurityTicket existingTicket = existing.get();
                log.info("[ORCHESTRATOR] Violation {} deja connue -> ticket #{}",
                        ticket.getSailpointViolationId(), existingTicket.getId());

                if (existingTicket.getStatus() != TicketStatus.CLOSED
                        && existingTicket.getStatus() != TicketStatus.CANCELLED) {
                    existingTicket.setDescription(ticket.getDescription());
                    existingTicket.setSeverity(ticket.getSeverity());
                    return ticketRepository.save(existingTicket);
                }
                return existingTicket;
            }
        }

        // ── Persister le nouveau ticket ──
        ticket.setStatus(TicketStatus.NEW);
        SecurityTicket saved = ticketRepository.save(ticket);
        log.info("[ORCHESTRATOR] Ticket #{} persisté", saved.getId());

        // ── Pousser vers ServiceNow (asynchrone) ──
        pushToServiceNowAsync(saved.getId());

        return saved;
    }

    /**
     * Crée un ticket depuis un audit CyberAudit7E avec un score critique.
     * Appelé par le FeedbackLoopListener quand le score est sous le seuil.
     * Le seuil est lu dynamiquement depuis system_settings.
     * 
     * @param reportId ID du rapport d'audit
     * @param siteUrl  URL du site audité
     * @param score    Score global de l'audit
     * @param details  Détails du rapport
     * @return Le ticket créé, ou empty si le score est au-dessus du seuil
     */
    
    @Transactional
    public Optional<SecurityTicket> createFromAudit(Long reportId, String siteUrl,
            double score, String details) {
        double threshold = getAutoTicketThreshold();

        if (score >= threshold) {
            log.debug("[ORCHESTRATOR] Score {} >= seuil {} — pas de ticket auto",
                    score, threshold);
            return Optional.empty();
        }

        log.info("[ORCHESTRATOR] Score {} < seuil {} — creation ticket auto",
                score, threshold);

        // Verifier doublon
        Optional<SecurityTicket> existing = ticketRepository.findByAuditReportId(reportId);
        if (existing.isPresent()) {
            log.info("[ORCHESTRATOR] Ticket existant pour le rapport #{}", reportId);
            SecurityTicket ex = existing.get();
            ex.setDescription(details);
            ex.setSeverity(TicketSeverity.fromAuditScore(score));
            return Optional.of(ticketRepository.save(ex));
        }

        SecurityTicket ticket = SecurityTicket.fromAudit(reportId, siteUrl, score, details);
        SecurityTicket saved = ticketRepository.save(ticket);
        log.info("[ORCHESTRATOR] Ticket #{} cree depuis audit #{} (score: {}, seuil: {})",
                saved.getId(), reportId, score, threshold);

        pushToServiceNowAsync(saved.getId());
        return Optional.of(saved);
    }

    /**
     * Pousse un ticket vers ServiceNow de façon asynchrone.
     * Crée l'incident et met à jour les références croisées (sys_id, number, url).
     */
    @Async("taskExecutor")
    @Transactional
    public void pushToServiceNowAsync(Long ticketId) {
        SecurityTicket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null || ticket.isSyncedWithServiceNow())
            return;

        log.info("[ORCHESTRATOR] Push vers ServiceNow — ticket #{}", ticketId);

        Optional<Map<String, String>> result = serviceNowClient.createIncident(ticket);
        result.ifPresent(snowResult -> {
            ticket.setServiceNowSysId(snowResult.get("sys_id"));
            ticket.setServiceNowNumber(snowResult.get("number"));
            ticket.setServiceNowUrl(snowResult.get("url"));
            ticket.setStatus(TicketStatus.OPEN);
            ticketRepository.save(ticket);
            log.info("[ORCHESTRATOR] Ticket #{} -> ServiceNow {} (sys_id: {})",
                    ticket.getId(), ticket.getServiceNowNumber(), ticket.getServiceNowSysId());
        });
    }

    /**
     * Synchronise les tickets non encore poussés vers ServiceNow.
     * Appelé manuellement ou par le scheduler.
     */
    @Transactional
    public int syncUnsyncedTickets() {
        var unsynced = ticketRepository.findUnsyncedWithServiceNow();
        log.info("[ORCHESTRATOR] {} ticket(s) non synchronise(s)", unsynced.size());

        int synced = 0;
        for (SecurityTicket ticket : unsynced) {
            Optional<Map<String, String>> result = serviceNowClient.createIncident(ticket);
            if (result.isPresent()) {
                ticket.setServiceNowSysId(result.get().get("sys_id"));
                ticket.setServiceNowNumber(result.get().get("number"));
                ticket.setServiceNowUrl(result.get().get("url"));
                ticket.setStatus(TicketStatus.OPEN);
                ticketRepository.save(ticket);
                synced++;
            }
        }
        log.info("[ORCHESTRATOR] {}/{} ticket(s) synchronise(s)", synced, unsynced.size());
        return synced;
    }

    /**
     * Résout un ticket dans CyberAudit7E ET dans ServiceNow.
     */
    @Transactional
    public SecurityTicket resolveTicket(Long ticketId, String closeNotes) {
        SecurityTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket #" + ticketId + " introuvable"));

        ticket.setStatus(TicketStatus.RESOLVED);
        ticketRepository.save(ticket);

        if (ticket.isSyncedWithServiceNow()) {
            serviceNowClient.resolveIncident(ticket.getServiceNowSysId(), closeNotes);
        }

        log.info("[ORCHESTRATOR] Ticket #{} resolu (SNOW: {})",
                ticketId, ticket.getServiceNowNumber());
        return ticket;
    }
}
