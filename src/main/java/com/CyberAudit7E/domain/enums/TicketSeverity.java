package com.cyberaudit7e.domain.enums;

/**
 * Sévérité d'un ticket de sécurité.
 * Alignée sur les niveaux de risque SailPoint (HIGH_RISK, MEDIUM_RISK, LOW_RISK)
 * et les priorités ServiceNow (1=Critical, 2=High, 3=Medium, 4=Low).
 */
public enum TicketSeverity {

    CRITICAL("Critique", 1, "HIGH_RISK"),
    HIGH("Haute", 2, "HIGH_RISK"),
    MEDIUM("Moyenne", 3, "MEDIUM_RISK"),
    LOW("Basse", 4, "LOW_RISK"),
    INFO("Information", 5, "LOW_RISK");

    private final String label;
    private final int snowPriority;
    private final String sailpointRiskLevel;

    TicketSeverity(String label, int snowPriority, String sailpointRiskLevel) {
        this.label = label;
        this.snowPriority = snowPriority;
        this.sailpointRiskLevel = sailpointRiskLevel;
    }

    public String getLabel() { return label; }
    public int getSnowPriority() { return snowPriority; }
    public String getSailpointRiskLevel() { return sailpointRiskLevel; }

    /**
     * Détermine la sévérité à partir d'un score d'audit (0.0-1.0).
     * Score bas = haute sévérité.
     */
    public static TicketSeverity fromAuditScore(double score) {
        if (score < 0.3) return CRITICAL;
        if (score < 0.5) return HIGH;
        if (score < 0.7) return MEDIUM;
        if (score < 0.9) return LOW;
        return INFO;
    }

    /**
     * Convertit un tag de risque SailPoint en sévérité.
     */
    public static TicketSeverity fromSailpointRisk(String riskTag) {
        if (riskTag == null) return MEDIUM;
        return switch (riskTag.toUpperCase()) {
            case "HIGH_RISK" -> HIGH;
            case "MEDIUM_RISK" -> MEDIUM;
            case "LOW_RISK" -> LOW;
            default -> MEDIUM;
        };
    }
}
