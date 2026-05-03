package com.cyberaudit7e.integration.webhook;

import com.cyberaudit7e.domain.entity.SecurityTicket;
import com.cyberaudit7e.domain.enums.TicketStatus;
import com.cyberaudit7e.repository.SecurityTicketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Webhook receiver pour les callbacks ServiceNow.
 *
 * ServiceNow peut envoyer des notifications quand un incident change
 * d'état, via une Business Rule (on Update) qui appelle ce webhook.
 *
 * Configuration côté ServiceNow :
 * 1. System Web Services → Outbound → REST Message
 * 2. Endpoint: https://cyberaudit7e.local/api/webhooks/servicenow
 * 3. Method: POST
 * 4. Business Rule sur "incident" (on Update, condition: state changes)
 * 5. Payload: { "sys_id": current.sys_id, "number": current.number,
 *               "state": current.state, "close_notes": current.close_notes }
 */
@RestController
@RequestMapping("/api/webhooks")
@Hidden
public class ServiceNowWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ServiceNowWebhookController.class);

    private final SecurityTicketRepository ticketRepository;
    private final ObjectMapper objectMapper;

    public ServiceNowWebhookController(SecurityTicketRepository ticketRepository,
                                        ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/webhooks/servicenow — Callback de mise à jour d'incident.
     *
     * Payload attendu :
     * {
     *   "sys_id": "abc123...",
     *   "number": "INC0012345",
     *   "state": "6",      // 1=New, 2=InProgress, 6=Resolved, 7=Closed
     *   "assigned_to": "John Doe",
     *   "close_notes": "Résolu par l'équipe sécurité",
     *   "close_code": "Solved (Permanently)"
     * }
     */
    @PostMapping("/servicenow")
    @Transactional
    public ResponseEntity<Map<String, Object>> receiveServiceNowCallback(
            @RequestBody String rawPayload) {

        log.info("[WEBHOOK-SNOW] Callback reçu ({} octets)", rawPayload.length());

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);

            String sysId = payload.has("sys_id") ? payload.get("sys_id").asText() : null;
            String number = payload.has("number") ? payload.get("number").asText() : null;
            int state = payload.has("state") ? payload.get("state").asInt() : 0;

            if (sysId == null && number == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "sys_id ou number requis"));
            }

            // Trouver le ticket correspondant
            Optional<SecurityTicket> ticketOpt = sysId != null
                    ? ticketRepository.findByServiceNowSysId(sysId)
                    : ticketRepository.findByServiceNowNumber(number);

            if (ticketOpt.isEmpty()) {
                log.warn("[WEBHOOK-SNOW] Ticket non trouvé pour sys_id={}, number={}", sysId, number);
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "ticket not found"));
            }

            SecurityTicket ticket = ticketOpt.get();
            TicketStatus oldStatus = ticket.getStatus();
            TicketStatus newStatus = TicketStatus.fromSnowState(state);

            // Mettre à jour le statut
            ticket.setStatus(newStatus);

            // Mettre à jour l'assignation si présente
            if (payload.has("assigned_to")) {
                ticket.setAssignedTo(payload.get("assigned_to").asText());
            }

            ticketRepository.save(ticket);

            log.info("[WEBHOOK-SNOW] Ticket #{} mis à jour : {} → {} (SNOW: {})",
                    ticket.getId(), oldStatus, newStatus, number);

            return ResponseEntity.ok(Map.of(
                    "status", "updated",
                    "ticketId", ticket.getId(),
                    "oldStatus", oldStatus.name(),
                    "newStatus", newStatus.name()
            ));

        } catch (Exception e) {
            log.error("[WEBHOOK-SNOW] Erreur : {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/webhooks/servicenow/test — Test de connectivité.
     */
    @GetMapping("/servicenow/test")
    public Map<String, Object> testEndpoint() {
        return Map.of("status", "ready", "service", "CyberAudit7E ServiceNow Webhook");
    }
}
