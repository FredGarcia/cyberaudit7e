package com.cyberaudit7e.dto;

import com.cyberaudit7e.domain.entity.AuditReport;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO résumé d'un rapport d'audit.
 * Évite l'exposition de l'entité JPA (lazy loading, circular refs).
 */
public record ReportSummaryDto(
        Long id,
        Long siteId,
        String siteName,
        Double scoreRgaa,
        Double scoreWcag,
        Double scoreDsfr,
        Double scoreGlobal,
        String completedPhase,
        String trend,
        int rulesCount,
        int passedCount,
        List<RuleResultDto> details,
        LocalDateTime auditedAt
) {

    public static ReportSummaryDto from(AuditReport report) {
        List<RuleResultDto> results = report.getRuleResults();
        int passed = results != null
                ? (int) results.stream().filter(RuleResultDto::passed).count()
                : 0;
        return new ReportSummaryDto(
                report.getId(),
                report.getSite().getId(),
                report.getSite().getName(),
                report.getScoreRgaa(),
                report.getScoreWcag(),
                report.getScoreDsfr(),
                report.getScoreGlobal(),
                report.getCompletedPhase() != null ? report.getCompletedPhase().name() : null,
                report.getTrend(),
                results != null ? results.size() : 0,
                passed,
                results,
                report.getAuditedAt()
        );
    }
}
