package com.cyberaudit7e.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration externalisée pour les intégrations ServiceNow et SailPoint.
 *
 * Lue depuis application.yml :
 *   cyberaudit7e.integrations.servicenow.*
 *   cyberaudit7e.integrations.sailpoint.*
 *
 * Les secrets sont injectés via variables d'environnement en prod :
 *   SNOW_CLIENT_ID, SNOW_CLIENT_SECRET, SP_CLIENT_ID, SP_CLIENT_SECRET
 */
@Configuration
@ConfigurationProperties(prefix = "cyberaudit7e.integrations")
public class IntegrationProperties {

    private ServiceNowConfig servicenow = new ServiceNowConfig();
    private SailPointConfig sailpoint = new SailPointConfig();

    public ServiceNowConfig getServicenow() { return servicenow; }
    public void setServicenow(ServiceNowConfig servicenow) { this.servicenow = servicenow; }
    public SailPointConfig getSailpoint() { return sailpoint; }
    public void setSailpoint(SailPointConfig sailpoint) { this.sailpoint = sailpoint; }

    // ── ServiceNow Configuration ──

    public static class ServiceNowConfig {
        /** Activer/désactiver l'intégration ServiceNow */
        private boolean enabled = false;
        /** Instance ServiceNow (ex: "mon-instance.service-now.com") */
        private String instance = "";
        /** OAuth 2.0 Client ID */
        private String clientId = "";
        /** OAuth 2.0 Client Secret */
        private String clientSecret = "";
        /** Username pour Basic Auth (fallback si OAuth non configuré) */
        private String username = "";
        /** Password pour Basic Auth */
        private String password = "";
        /** Méthode d'authentification : "oauth" ou "basic" */
        private String authMethod = "basic";
        /** Assignment group par défaut pour les incidents créés */
        private String assignmentGroup = "Accessibility Team";
        /** Catégorie par défaut pour les incidents */
        private String defaultCategory = "Security";
        /** Sous-catégorie par défaut */
        private String defaultSubcategory = "Vulnerability";
        /** Timeout des appels HTTP en secondes */
        private int timeout = 30;

        // Getters & Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getInstance() { return instance; }
        public void setInstance(String instance) { this.instance = instance; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getAuthMethod() { return authMethod; }
        public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
        public String getAssignmentGroup() { return assignmentGroup; }
        public void setAssignmentGroup(String g) { this.assignmentGroup = g; }
        public String getDefaultCategory() { return defaultCategory; }
        public void setDefaultCategory(String c) { this.defaultCategory = c; }
        public String getDefaultSubcategory() { return defaultSubcategory; }
        public void setDefaultSubcategory(String s) { this.defaultSubcategory = s; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        /** URL de base de l'API ServiceNow */
        public String getBaseUrl() {
            return "https://" + instance;
        }
    }

    // ── SailPoint Configuration ──

    public static class SailPointConfig {
        /** Activer/désactiver l'intégration SailPoint */
        private boolean enabled = false;
        /** Tenant SailPoint (ex: "mon-tenant") */
        private String tenant = "";
        /** Personal Access Token — Client ID */
        private String clientId = "";
        /** Personal Access Token — Client Secret */
        private String clientSecret = "";
        /** Secret partagé pour valider les webhooks entrants */
        private String webhookSecret = "";
        /** Timeout en secondes */
        private int timeout = 30;

        // Getters & Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTenant() { return tenant; }
        public void setTenant(String tenant) { this.tenant = tenant; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String s) { this.webhookSecret = s; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        /** URL de base de l'API SailPoint */
        public String getBaseUrl() {
            return "https://" + tenant + ".api.identitynow.com";
        }
    }
}
