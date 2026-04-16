package com.CyberAudit7E.domain.rule;

import com.CyberAudit7E.domain.enums.RuleCategory;
import com.CyberAudit7E.dto.RuleResultDto;

/**
 * Contrat Strategy Pattern pour les règles d'audit.
 *
 * Chaque implémentation est un @Component Spring : le conteneur IoC
 * collecte automatiquement toutes les implémentations et les injecte
 * dans AuditEngine via List<AuditRule>.
 *
 * Inspiré du moteur à 17 règles d'AuditAccess (Django),
 * transposé en Java idiomatique.
 */
public interface AuditRule {

    /** Identifiant unique de la règle (ex: "RGAA-8.5", "WCAG-1.3.1") */
    String id();

    /** Description humaine de la règle */
    String description();

    /** Catégorie pour le scoring pondéré */
    RuleCategory category();

    /**
     * Évalue la règle sur l'URL donnée.
     * En mode POC, l'évaluation est simulée.
     * En production, elle utilisera un crawler HTTP (Jsoup/Playwright).
     *
     * @param url URL du site à auditer
     * @return Résultat de l'évaluation avec score 0.0-1.0
     */
    RuleResultDto evaluate(String url);
}
