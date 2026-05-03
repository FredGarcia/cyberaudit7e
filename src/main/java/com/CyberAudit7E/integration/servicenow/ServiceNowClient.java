package com.cyberaudit7e.integration.servicenow;

import com.cyberaudit7e.config.IntegrationProperties;
import com.cyberaudit7e.domain.entity.SecurityTicket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client REST pour ServiceNow Table API.
 *
 * Fonctionnalités :
 * - Authentification OAuth 2.0 avec cache du token
 * - Fallback Basic Auth si OAuth non configuré
 * - Création d'incidents (POST /api/now/table/incident)
 * - Mise à jour d'incidents (PATCH /api/now/table/incident/{sys_id})
 * - Consultation d'incidents (GET /api/now/table/incident/{sys_id})
 * - Résolution/fermeture d'incidents
 *
 * API utilisée : ServiceNow Table API
 * Documentation : https://docs.servicenow.com/bundle/latest/page/integrate/inbound-rest/concept/c_TableAPI.html
 */
@Service
public class ServiceNowClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceNowClient.class);
    private static final String TABLE_API = "/api/now/table/incident";

    private final IntegrationProperties.ServiceNowConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Cache du token OAuth */
    private String cachedToken;
    private long tokenExpiresAt = 0;

    public ServiceNowClient(IntegrationProperties properties,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.config = properties.getServicenow();
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Crée un incident dans ServiceNow à partir d'un SecurityTicket.
     * Retourne le sys_id et le numéro de l'incident créé.
     *
     * @param ticket Le ticket à pousser vers ServiceNow
     * @return Map avec "sys_id" et "number", ou empty si échec/désactivé
     */
    public Optional<Map<String, String>> createIncident(SecurityTicket ticket) {
        if (!config.isEnabled()) {
            log.debug("[SNOW] Intégration désactivée — incident non créé");
            return Optional.empty();
        }

        Map<String, Object> body = buildIncidentBody(ticket);
        String url = config.getBaseUrl() + TABLE_API;

        log.info("[SNOW] Création incident → {} | titre: {}", url, ticket.getTitle());

        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                JsonNode result = objectMapper.readTree(response.getBody()).get("result");
                String sysId = result.get("sys_id").asText();
                String number = result.get("number").asText();

                log.info("[SNOW] Incident créé : {} (sys_id: {})", number, sysId);
                return Optional.of(Map.of(
                        "sys_id", sysId,
                        "number", number,
                        "url", config.getBaseUrl() + "/incident.do?sys_id=" + sysId
                ));
            }

            log.warn("[SNOW] Réponse inattendue : {}", response.getStatusCode());
            return Optional.empty();

        } catch (RestClientException e) {
            log.error("[SNOW] Erreur création incident : {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("[SNOW] Erreur parsing réponse : {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Met à jour un incident existant dans ServiceNow.
     */
    public boolean updateIncident(String sysId, Map<String, Object> fields) {
        if (!config.isEnabled() || sysId == null) return false;

        String url = config.getBaseUrl() + TABLE_API + "/" + sysId;
        log.info("[SNOW] Mise à jour incident {} : {}", sysId, fields.keySet());

        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(fields, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PATCH, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) log.info("[SNOW] Incident {} mis à jour", sysId);
            return success;

        } catch (Exception e) {
            log.error("[SNOW] Erreur mise à jour {} : {}", sysId, e.getMessage());
            return false;
        }
    }

    /**
     * Récupère les détails d'un incident ServiceNow.
     */
    public Optional<JsonNode> getIncident(String sysId) {
        if (!config.isEnabled() || sysId == null) return Optional.empty();

        String url = config.getBaseUrl() + TABLE_API + "/" + sysId;

        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(objectMapper.readTree(response.getBody()).get("result"));
            }
        } catch (Exception e) {
            log.error("[SNOW] Erreur lecture {} : {}", sysId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Résout un incident dans ServiceNow (state=6, close_code, close_notes).
     */
    public boolean resolveIncident(String sysId, String closeNotes) {
        return updateIncident(sysId, Map.of(
                "state", "6",
                "close_code", "Solved (Permanently)",
                "close_notes", closeNotes != null ? closeNotes : "Résolu via CyberAudit7E"
        ));
    }

    /**
     * Vérifie la connectivité avec ServiceNow.
     */
    public boolean testConnection() {
        if (!config.isEnabled()) return false;
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = config.getBaseUrl() + "/api/now/table/incident?sysparm_limit=1";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("[SNOW] Test connexion échoué : {}", e.getMessage());
            return false;
        }
    }

    // ── Construction du body incident ──

    private Map<String, Object> buildIncidentBody(SecurityTicket ticket) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("short_description", truncate(ticket.getTitle(), 160));
        body.put("description", buildDescription(ticket));
        body.put("priority", String.valueOf(ticket.getSeverity().getSnowPriority()));
        body.put("impact", ticket.getSeverity().getSnowPriority() <= 2 ? "1" : "2");
        body.put("urgency", ticket.getSeverity().getSnowPriority() <= 2 ? "1" : "2");
        body.put("category", config.getDefaultCategory());
        body.put("subcategory", config.getDefaultSubcategory());
        body.put("assignment_group", config.getAssignmentGroup());

        // Catégorisation selon la source
        if (ticket.getSource() != null) {
            body.put("u_source_system", ticket.getSource().getLabel());
        }
        if (ticket.getSailpointIdentityName() != null) {
            body.put("u_affected_user", ticket.getSailpointIdentityName());
        }
        if (ticket.getSiteUrl() != null) {
            body.put("u_affected_resource", ticket.getSiteUrl());
        }

        return body;
    }

    private String buildDescription(SecurityTicket ticket) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CyberAudit7E — Ticket #").append(ticket.getId()).append(" ===\n\n");
        sb.append("Source : ").append(ticket.getSource().getLabel()).append("\n");
        sb.append("Sévérité : ").append(ticket.getSeverity().getLabel()).append("\n");
        sb.append("Catégorie : ").append(ticket.getCategory()).append("\n\n");

        if (ticket.getDescription() != null) {
            sb.append(ticket.getDescription()).append("\n\n");
        }
        if (ticket.getSailpointViolationId() != null) {
            sb.append("SailPoint Violation ID : ").append(ticket.getSailpointViolationId()).append("\n");
        }
        if (ticket.getSailpointIdentityName() != null) {
            sb.append("Identité SailPoint : ").append(ticket.getSailpointIdentityName()).append("\n");
        }
        if (ticket.getAuditReportId() != null) {
            sb.append("Rapport d'audit : #").append(ticket.getAuditReportId()).append("\n");
        }
        if (ticket.getSiteUrl() != null) {
            sb.append("Site concerné : ").append(ticket.getSiteUrl()).append("\n");
        }

        return sb.toString();
    }

    // ── Authentification ──

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        if ("oauth".equals(config.getAuthMethod())) {
            String token = getOAuthToken();
            headers.setBearerAuth(token);
        } else {
            headers.setBasicAuth(config.getUsername(), config.getPassword());
        }

        return headers;
    }

    /**
     * Obtient un token OAuth 2.0 ServiceNow (avec cache).
     */
    private String getOAuthToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedToken;
        }

        String tokenUrl = config.getBaseUrl() + "/oauth_token.do";
        log.debug("[SNOW] Demande de token OAuth → {}", tokenUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            cachedToken = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
            tokenExpiresAt = System.currentTimeMillis() + Duration.ofSeconds(expiresIn - 60).toMillis();

            log.info("[SNOW] Token OAuth obtenu (expire dans {}s)", expiresIn);
            return cachedToken;

        } catch (Exception e) {
            log.error("[SNOW] Échec obtention token OAuth : {}", e.getMessage());
            throw new RuntimeException("ServiceNow OAuth failed", e);
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
