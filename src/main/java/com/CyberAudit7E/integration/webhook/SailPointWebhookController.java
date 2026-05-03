package com.cyberaudit7e.integration.webhook;

import com.cyberaudit7e.config.IntegrationProperties;
import com.cyberaudit7e.domain.entity.SecurityTicket;
import com.cyberaudit7e.integration.TicketOrchestrator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook receiver pour les Event Triggers SailPoint.
 *
 * SailPoint Identity Security Cloud envoie des HTTP POST à cet endpoint
 * quand un événement se produit (violation de policy, changement d'identité, etc.)
 *
 * Configuration côté SailPoint :
 * 1. Admin → Event Triggers → Sélectionner un trigger
 * 2. + Subscribe → Type: HTTP
 * 3. Integration URL: https://cyberaudit7e.local/api/webhooks/sailpoint
 * 4. Auth Type: Bearer Token (utiliser le webhookSecret configuré)
 *
 * Événements supportés :
 * - idn:policy-violation          → Violation SoD
 * - idn:identity-deleted          → Identité supprimée (compte orphelin)
 * - idn:access-request-pre-approval  → Demande d'accès pré-approbation
 * - idn:access-request-post-approval → Demande d'accès post-approbation
 * - idn:account-aggregation-completed → Agrégation terminée
 * - idn:certification-signed-off     → Certification signée
 *
 * Payload SailPoint (structure commune) :
 * {
 *   "_metadata": { "triggerId": "idn:policy-violation", "invocationId": "..." },
 *   "identity": { "id": "...", "name": "John Doe", "type": "IDENTITY" },
 *   "policyName": "SoD - Finance",
 *   "violatingAccessItems": [...],
 *   ...
 * }
 */
@RestController
@RequestMapping("/api/webhooks")
@Hidden  // Caché du Swagger public
public class SailPointWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SailPointWebhookController.class);

    private final IntegrationProperties.SailPointConfig config;
    private final TicketOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    public SailPointWebhookController(IntegrationProperties properties,
                                       TicketOrchestrator orchestrator,
                                       ObjectMapper objectMapper) {
        this.config = properties.getSailpoint();
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /api/webhooks/sailpoint — Réception des Event Triggers SailPoint.
     *
     * Flux :
     * 1. Valider le token d'authentification (Bearer)
     * 2. Parser le payload JSON
     * 3. Extraire le type d'événement et les données
     * 4. Créer un SecurityTicket via l'orchestrateur
     * 5. Retourner 200 OK (SailPoint attend un 2xx)
     */
    @PostMapping("/sailpoint")
    public ResponseEntity<Map<String, Object>> receiveSailPointEvent(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String rawPayload) {

        log.info("[WEBHOOK-SP] Événement reçu ({} octets)", rawPayload.length());

        // ── 1. Validation du token ──
        if (!validateToken(authorization)) {
            log.warn("[WEBHOOK-SP] Token invalide — rejet");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }

        try {
            // ── 2. Parser le payload ──
            JsonNode payload = objectMapper.readTree(rawPayload);

            // ── 3. Extraire les données ──
            String triggerId = extractTriggerId(payload);
            String invocationId = extractField(payload, "_metadata", "invocationId");

            log.info("[WEBHOOK-SP] Trigger: {} | Invocation: {}", triggerId, invocationId);

            // ── 4. Mapper selon le type d'événement ──
            SecurityTicket ticket = mapToTicket(triggerId, payload, rawPayload);

            if (ticket != null) {
                // ── 5. Orchestrer (persister + pousser vers ServiceNow) ──
                SecurityTicket saved = orchestrator.processIncomingTicket(ticket);

                log.info("[WEBHOOK-SP] Ticket #{} créé — source: {}, sévérité: {}",
                        saved.getId(), saved.getSource(), saved.getSeverity());

                return ResponseEntity.ok(Map.of(
                        "status", "accepted",
                        "ticketId", saved.getId(),
                        "serviceNowNumber", saved.getServiceNowNumber() != null
                                ? saved.getServiceNowNumber() : "pending"
                ));
            }

            log.info("[WEBHOOK-SP] Événement {} ignoré (non mappé)", triggerId);
            return ResponseEntity.ok(Map.of("status", "ignored", "trigger", triggerId));

        } catch (Exception e) {
            log.error("[WEBHOOK-SP] Erreur traitement : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/webhooks/sailpoint/test — Endpoint de test (SailPoint peut vérifier la connectivité).
     */
    @GetMapping("/sailpoint/test")
    public Map<String, Object> testEndpoint() {
        return Map.of(
                "status", "ready",
                "service", "CyberAudit7E Webhook Receiver",
                "sailpointEnabled", config.isEnabled()
        );
    }

    // ── Mapping événement → SecurityTicket ──

    private SecurityTicket mapToTicket(String triggerId, JsonNode payload, String rawPayload) {
        if (triggerId == null) return null;

        return switch (triggerId) {
            case "idn:policy-violation" -> mapPolicyViolation(payload, rawPayload);
            case "idn:identity-deleted" -> mapIdentityDeleted(payload, rawPayload);
            case "idn:access-request-pre-approval",
                 "idn:access-request-post-approval" -> mapAccessRequest(triggerId, payload, rawPayload);
            case "idn:account-aggregation-completed" -> mapAccountAggregation(payload, rawPayload);
            case "idn:certification-signed-off" -> mapCertification(payload, rawPayload);
            default -> mapGenericEvent(triggerId, payload, rawPayload);
        };
    }

    private SecurityTicket mapPolicyViolation(JsonNode payload, String raw) {
        String identityId = extractField(payload, "identity", "id");
        String identityName = extractField(payload, "identity", "name");
        String policyName = payload.has("policyName") ? payload.get("policyName").asText() : "Politique inconnue";

        String description = String.format(
                "Violation de politique détectée par SailPoint.\n\n" +
                "Politique : %s\n" +
                "Identité : %s (ID: %s)\n",
                policyName, identityName, identityId);

        // Détecter le niveau de risque depuis les tags
        String riskTag = "HIGH_RISK"; // Les violations SoD sont toujours high risk
        if (payload.has("violatingAccessItems")) {
            description += "Accès en violation : " + payload.get("violatingAccessItems").size() + " item(s)\n";
        }

        SecurityTicket ticket = SecurityTicket.fromSailpoint(
                "idn:policy-violation", identityId, identityId, identityName,
                riskTag, description);
        ticket.setRawPayload(raw);
        return ticket;
    }

    private SecurityTicket mapIdentityDeleted(JsonNode payload, String raw) {
        String identityId = extractField(payload, "identity", "id");
        String identityName = extractField(payload, "identity", "name");

        SecurityTicket ticket = SecurityTicket.fromSailpoint(
                "idn:identity-deleted", identityId, identityId, identityName,
                "MEDIUM_RISK",
                "Identité supprimée dans SailPoint. Vérifier les comptes orphelins associés.");
        ticket.setRawPayload(raw);
        return ticket;
    }

    private SecurityTicket mapAccessRequest(String triggerId, JsonNode payload, String raw) {
        String identityId = extractField(payload, "requestedFor", "id");
        String identityName = extractField(payload, "requestedFor", "name");

        StringBuilder desc = new StringBuilder("Demande d'accès détectée par SailPoint.\n\n");
        if (payload.has("requestedItems") && payload.get("requestedItems").isArray()) {
            for (JsonNode item : payload.get("requestedItems")) {
                desc.append("- ").append(item.has("name") ? item.get("name").asText() : "?")
                    .append(" (").append(item.has("type") ? item.get("type").asText() : "?").append(")\n");
            }
        }

        SecurityTicket ticket = SecurityTicket.fromSailpoint(
                triggerId, extractField(payload, "accessRequestId", null),
                identityId, identityName, "MEDIUM_RISK", desc.toString());
        ticket.setRawPayload(raw);
        return ticket;
    }

    private SecurityTicket mapAccountAggregation(JsonNode payload, String raw) {
        String sourceName = payload.has("source") && payload.get("source").has("name")
                ? payload.get("source").get("name").asText() : "Source inconnue";

        SecurityTicket ticket = SecurityTicket.fromSailpoint(
                "idn:account-aggregation-completed", null, null, null,
                "LOW_RISK",
                "Agrégation de comptes terminée pour la source : " + sourceName);
        ticket.setRawPayload(raw);
        return ticket;
    }

    private SecurityTicket mapCertification(JsonNode payload, String raw) {
        String certName = payload.has("certification") && payload.get("certification").has("name")
                ? payload.get("certification").get("name").asText() : "Certification inconnue";

        SecurityTicket ticket = SecurityTicket.fromSailpoint(
                "idn:certification-signed-off", null, null, null,
                "LOW_RISK",
                "Campagne de certification signée : " + certName);
        ticket.setRawPayload(raw);
        return ticket;
    }

    private SecurityTicket mapGenericEvent(String triggerId, JsonNode payload, String raw) {
        SecurityTicket ticket = SecurityTicket.fromSailpoint(
                triggerId, null, null, null,
                "LOW_RISK",
                "Événement SailPoint : " + triggerId);
        ticket.setRawPayload(raw);
        return ticket;
    }

    // ── Helpers ──

    private boolean validateToken(String authorization) {
        if (!config.isEnabled()) return true; // Accept all if disabled (dev mode)
        if (config.getWebhookSecret() == null || config.getWebhookSecret().isBlank()) return true;

        if (authorization == null || !authorization.startsWith("Bearer ")) return false;
        String token = authorization.substring(7);
        return config.getWebhookSecret().equals(token);
    }

    private String extractTriggerId(JsonNode payload) {
        if (payload.has("_metadata") && payload.get("_metadata").has("triggerId")) {
            return payload.get("_metadata").get("triggerId").asText();
        }
        if (payload.has("triggerId")) return payload.get("triggerId").asText();
        if (payload.has("type")) return payload.get("type").asText();
        return null;
    }

    private String extractField(JsonNode payload, String parent, String field) {
        if (parent != null && payload.has(parent)) {
            JsonNode node = payload.get(parent);
            if (field != null && node.has(field)) return node.get(field).asText();
            if (field == null && node.isTextual()) return node.asText();
        }
        if (field != null && payload.has(field)) return payload.get(field).asText();
        return null;
    }
}
