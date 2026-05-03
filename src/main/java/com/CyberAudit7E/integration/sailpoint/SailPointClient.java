package com.cyberaudit7e.integration.sailpoint;

import com.cyberaudit7e.config.IntegrationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Client REST pour SailPoint Identity Security Cloud (IdentityNow) V3 API.
 *
 * Fonctionnalités :
 * - Authentification OAuth 2.0 via Personal Access Token
 * - Recherche d'identités (GET /v3/public-identities)
 * - Détails d'une identité (GET /v3/identities/{id})
 * - Recherche de violations (GET /beta/policy-violations)
 * - Gestion des access profiles (GET /v3/access-profiles)
 *
 * API : https://developer.sailpoint.com/docs/api/v3/
 * Auth : https://developer.sailpoint.com/docs/api/getting-started/
 *
 * Note : le webhook ENTRANT (SailPoint → CyberAudit7E) est géré
 * par SailPointWebhookController, pas par ce client.
 * Ce client est pour les appels SORTANTS (CyberAudit7E → SailPoint).
 */
@Service
public class SailPointClient {

    private static final Logger log = LoggerFactory.getLogger(SailPointClient.class);

    private final IntegrationProperties.SailPointConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Cache du token OAuth */
    private String cachedToken;
    private long tokenExpiresAt = 0;

    public SailPointClient(IntegrationProperties properties,
                           RestTemplate restTemplate,
                           ObjectMapper objectMapper) {
        this.config = properties.getSailpoint();
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Recherche une identité par nom ou email.
     * GET /v3/public-identities?filters=name eq "{query}" OR email eq "{query}"
     */
    public Optional<JsonNode> searchIdentity(String query) {
        if (!config.isEnabled()) return Optional.empty();

        String url = config.getBaseUrl() + "/v3/public-identities?limit=5&filters="
                + "name co \"" + query + "\"";

        log.info("[SAILPOINT] Recherche identité : {}", query);
        return apiGet(url);
    }

    /**
     * Récupère les détails d'une identité par son ID.
     * GET /v3/identities/{id}
     */
    public Optional<JsonNode> getIdentity(String identityId) {
        if (!config.isEnabled() || identityId == null) return Optional.empty();

        String url = config.getBaseUrl() + "/v3/identities/" + identityId;
        log.debug("[SAILPOINT] Détail identité : {}", identityId);
        return apiGet(url);
    }

    /**
     * Liste les violations de policy actives.
     * GET /beta/policy-violations
     */
    public Optional<JsonNode> listPolicyViolations() {
        if (!config.isEnabled()) return Optional.empty();

        String url = config.getBaseUrl() + "/beta/policy-violations?limit=50";
        log.info("[SAILPOINT] Liste des violations de politique");
        return apiGet(url);
    }

    /**
     * Récupère le détail d'une violation par son ID.
     */
    public Optional<JsonNode> getViolation(String violationId) {
        if (!config.isEnabled() || violationId == null) return Optional.empty();

        String url = config.getBaseUrl() + "/beta/policy-violations/" + violationId;
        log.debug("[SAILPOINT] Détail violation : {}", violationId);
        return apiGet(url);
    }

    /**
     * Liste les access profiles.
     */
    public Optional<JsonNode> listAccessProfiles(int limit) {
        if (!config.isEnabled()) return Optional.empty();

        String url = config.getBaseUrl() + "/v3/access-profiles?limit=" + limit;
        return apiGet(url);
    }

    /**
     * Liste les event trigger subscriptions configurées.
     */
    public Optional<JsonNode> listTriggerSubscriptions() {
        if (!config.isEnabled()) return Optional.empty();

        String url = config.getBaseUrl() + "/beta/trigger-subscriptions";
        return apiGet(url);
    }

    /**
     * Vérifie la connectivité avec SailPoint.
     */
    public boolean testConnection() {
        if (!config.isEnabled()) return false;
        try {
            String url = config.getBaseUrl() + "/v3/public-identities?limit=1";
            Optional<JsonNode> result = apiGet(url);
            return result.isPresent();
        } catch (Exception e) {
            log.error("[SAILPOINT] Test connexion échoué : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Retourne les infos de configuration (sans secrets).
     */
    public java.util.Map<String, Object> getConnectionInfo() {
        return java.util.Map.of(
                "enabled", config.isEnabled(),
                "tenant", config.getTenant(),
                "baseUrl", config.getBaseUrl(),
                "connected", config.isEnabled() && testConnection()
        );
    }

    // ── Appels API génériques ──

    private Optional<JsonNode> apiGet(String url) {
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(objectMapper.readTree(response.getBody()));
            }

            log.warn("[SAILPOINT] Réponse {} pour {}", response.getStatusCode(), url);
            return Optional.empty();

        } catch (Exception e) {
            log.error("[SAILPOINT] Erreur API {} : {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    // ── Authentification OAuth 2.0 ──

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.setBearerAuth(getOAuthToken());
        return headers;
    }

    /**
     * Obtient un token OAuth via Personal Access Token (client_credentials).
     * POST https://{tenant}.api.identitynow.com/oauth/token
     *   ?grant_type=client_credentials
     *   &client_id={client_id}
     *   &client_secret={client_secret}
     */
    private String getOAuthToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cachedToken;
        }

        String tokenUrl = config.getBaseUrl() + "/oauth/token"
                + "?grant_type=client_credentials"
                + "&client_id=" + config.getClientId()
                + "&client_secret=" + config.getClientSecret();

        log.debug("[SAILPOINT] Demande de token OAuth");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            cachedToken = json.get("access_token").asText();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").asInt() : 3600;
            tokenExpiresAt = System.currentTimeMillis() + Duration.ofSeconds(expiresIn - 60).toMillis();

            log.info("[SAILPOINT] Token OAuth obtenu (expire dans {}s)", expiresIn);
            return cachedToken;

        } catch (Exception e) {
            log.error("[SAILPOINT] Échec obtention token : {}", e.getMessage());
            throw new RuntimeException("SailPoint OAuth failed", e);
        }
    }
}
