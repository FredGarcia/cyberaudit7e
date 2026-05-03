package com.cyberaudit7e.domain.enums;

/**
 * Statut d'un ticket de sécurité dans son cycle de vie.
 * Aligné sur les états ServiceNow (New→In Progress→Resolved→Closed)
 * et les états SailPoint (Pending→Approved→Completed).
 */
public enum TicketStatus {

    NEW("Nouveau", 1),
    OPEN("Ouvert", 2),
    IN_PROGRESS("En cours", 3),
    PENDING_VALIDATION("En attente de validation", 4),
    RESOLVED("Résolu", 5),
    CLOSED("Fermé", 6),
    CANCELLED("Annulé", 7);

    private final String label;
    private final int snowStateCode;  // Mapping ServiceNow incident state

    TicketStatus(String label, int snowStateCode) {
        this.label = label;
        this.snowStateCode = snowStateCode;
    }

    public String getLabel() { return label; }
    public int getSnowStateCode() { return snowStateCode; }

    /**
     * Convertit un état ServiceNow (1-7) en TicketStatus.
     */
    public static TicketStatus fromSnowState(int state) {
        for (TicketStatus s : values()) {
            if (s.snowStateCode == state) return s;
        }
        return OPEN;
    }
}
