package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;

/**
 * Contrat Strategy Pattern pour les règles d'audit.
 *
 * M4 : la méthode evaluate() reçoit un AuditContext au lieu d'une String url.
 * Le contexte contient l'URL ET le Document Jsoup parsé (si disponible).
 *
 * Chaque implémentation est un @Component Spring auto-injecté dans AuditEngine.
 * Ajouter une règle = créer un @Component, zéro modification ailleurs.
 */
public interface AuditRule {

    /** Identifiant unique de la règle (ex: "RGAA-8.5", "WCAG-1.3.1") */
    String id();

    /** Description humaine de la règle */
    String description();

    /** Catégorie pour le scoring pondéré */
    RuleCategory category();

    /**
     * Priorité d'exécution (plus bas = exécuté en premier).
     * Les règles structurelles (titre, lang) passent avant les règles de contenu.
     * Par défaut : 100 (priorité normale).
     */
    default int priority() {
        return 100;
    }

    /**
     * Évalue la règle sur le contexte d'audit.
     *
     * @param context Contexte contenant l'URL et le DOM parsé (optionnel)
     * @return Résultat avec score 0.0-1.0
     */
    RuleResultDto evaluate(AuditContext context);
}
