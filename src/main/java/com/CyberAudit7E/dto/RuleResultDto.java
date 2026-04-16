package com.cyberaudit7e.dto;

import com.cyberaudit7e.domain.enums.RuleCategory;

/**
 * Résultat d'évaluation d'une règle d'audit.
 * Utilise un Java Record (immutable par design).
 *
 * @param ruleId    Identifiant de la règle (ex: "RGAA-8.5")
 * @param category  Catégorie pour le scoring pondéré
 * @param passed    true si la règle est satisfaite
 * @param score     Score entre 0.0 (échec) et 1.0 (succès)
 * @param detail    Message descriptif du résultat
 */
public record RuleResultDto(
        String ruleId,
        RuleCategory category,
        boolean passed,
        double score,
        String detail
) {

    /**
     * Factory method pour un succès.
     */
    public static RuleResultDto success(String ruleId, RuleCategory category, String detail) {
        return new RuleResultDto(ruleId, category, true, 1.0, detail);
    }

    /**
     * Factory method pour un échec.
     */
    public static RuleResultDto failure(String ruleId, RuleCategory category, String detail) {
        return new RuleResultDto(ruleId, category, false, 0.0, detail);
    }

    /**
     * Factory method pour un résultat partiel.
     */
    public static RuleResultDto partial(String ruleId, RuleCategory category, double score, String detail) {
        return new RuleResultDto(ruleId, category, score >= 0.5, score, detail);
    }
}
