package com.CyberAudit7E.event;

import com.CyberAudit7E.domain.entity.AuditReport;
import org.springframework.context.ApplicationEvent;

/**
 * Événement émis à la fin d'un cycle d'audit complet.
 * Phase 7E : ÉMETTRE
 *
 * Équivalent Spring du Redis Streams bridge (DB5→DB2)
 * utilisé dans AuditAccess pour la communication Django→Celery.
 * Avantage : zéro infrastructure externe.
 *
 * Les listeners (@EventListener) réagissent à cet événement
 * pour implémenter la rétroaction (phase ÉQUILIBRER).
 */
public class AuditCompletedEvent extends ApplicationEvent {

    private final AuditReport report;
    private final String trend;

    public AuditCompletedEvent(Object source, AuditReport report, String trend) {
        super(source);
        this.report = report;
        this.trend = trend;
    }

    public AuditReport getReport() {
        return report;
    }

    public String getTrend() {
        return trend;
    }
}
