package com.cyberaudit7e.domain.enums;

/**
 * Source d'origine d'un ticket de sécurité.
 * Permet de tracer d'où vient chaque ticket dans le flux triangulaire
 * SailPoint ↔ CyberAudit7E ↔ ServiceNow.
 */
public enum TicketSource {

    AUDIT("CyberAudit7E", "Détecté par un audit d'accessibilité"),
    SAILPOINT("SailPoint", "Violation d'identité détectée par SailPoint"),
    SERVICENOW("ServiceNow", "Incident créé manuellement dans ServiceNow"),
    MANUAL("Manuel", "Créé manuellement dans CyberAudit7E");

    private final String label;
    private final String description;

    TicketSource(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
