package com.CyberAudit7E.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO de réponse d'un audit complet (cycle 7E).
 *
 * @param reportId     ID du rapport généré
 * @param siteUrl      URL auditée
 * @param siteName     Nom du site
 * @param scores       Scores par catégorie + global
 * @param currentPhase Phase 7E atteinte à la fin du cycle
 * @param rulesCount   Nombre de règles évaluées
 * @param passedCount  Nombre de règles réussies
 * @param details      Résultats détaillés de chaque règle
 * @param auditedAt    Horodatage de l'audit
 */
public record AuditResponseDto(
        Long reportId,
        String siteUrl,
        String siteName,
        Map<String, Double> scores,
        String currentPhase,
        int rulesCount,
        int passedCount,
        List<RuleResultDto> details,
        LocalDateTime auditedAt
) {

    /**
     * Factory depuis un AuditReport et ses résultats.
     */
    public static AuditResponseDto from(
            Long reportId,
            String siteUrl,
            String siteName,
            Map<String, Double> scores,
            String currentPhase,
            List<RuleResultDto> details,
            LocalDateTime auditedAt
    ) {
        int passed = (int) details.stream().filter(RuleResultDto::passed).count();
        return new AuditResponseDto(
                reportId, siteUrl, siteName, scores,
                currentPhase, details.size(), passed,
                details, auditedAt
        );
    }
}
