package com.CyberAudit7E.domain.enums;

/**
 * Catégories de règles d'audit — alignées sur les référentiels AuditAccess.
 * La pondération du scoring composite est définie dans ScoringService.
 */
public enum RuleCategory {

    RGAA("RGAA 4.1", "Référentiel Général d'Amélioration de l'Accessibilité", 0.5),
    WCAG("WCAG 2.2", "Web Content Accessibility Guidelines", 0.3),
    DSFR("DSFR", "Design System de l'État Français", 0.2);

    private final String code;
    private final String label;
    private final double defaultWeight;

    RuleCategory(String code, String label, double defaultWeight) {
        this.code = code;
        this.label = label;
        this.defaultWeight = defaultWeight;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Poids par défaut dans la formule de scoring :
     * score = RGAA×0.5 + WCAG×0.3 + DSFR×0.2
     */
    public double getDefaultWeight() {
        return defaultWeight;
    }
}
