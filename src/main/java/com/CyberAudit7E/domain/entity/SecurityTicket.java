package com.cyberaudit7e.domain.entity;

import com.cyberaudit7e.domain.enums.TicketSeverity;
import com.cyberaudit7e.domain.enums.TicketSource;
import com.cyberaudit7e.domain.enums.TicketStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Ticket de sécurité unifié — le pont entre les trois systèmes.
 *
 * Un SecurityTicket peut être créé par :
 * - CyberAudit7E (audit d'accessibilité avec score critique)
 * - SailPoint (violation de policy, SoD, compte orphelin)
 * - ServiceNow (incident créé manuellement)
 *
 * Il maintient les références croisées :
 * - sailpointViolationId → ID de la violation dans SailPoint
 * - serviceNowSysId → sys_id de l'incident dans ServiceNow
 * - serviceNowNumber → numéro lisible (INC0012345)
 * - auditReportId → ID du rapport d'audit CyberAudit7E
 *
 * Le cycle de vie suit l'Axiome 7E :
 * NEW → OPEN → IN_PROGRESS → PENDING_VALIDATION → RESOLVED → CLOSED
 */
@Entity
@Table(name = "security_tickets")
public class SecurityTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identité du ticket ──

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "CLOB")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketSeverity severity;

    @Column(length = 100)
    private String category; // ex: "accessibility", "sod_violation", "orphan_account"

    // ── Références croisées ──

    /** ID de la violation dans SailPoint IdentityNow (nullable si source != SAILPOINT) */
    @Column(name = "sailpoint_violation_id", length = 100)
    private String sailpointViolationId;

    /** Type d'événement SailPoint (ex: "idn:policy-violation", "idn:identity-deleted") */
    @Column(name = "sailpoint_event_type", length = 100)
    private String sailpointEventType;

    /** ID SailPoint de l'identité concernée */
    @Column(name = "sailpoint_identity_id", length = 100)
    private String sailpointIdentityId;

    /** Nom de l'identité SailPoint */
    @Column(name = "sailpoint_identity_name", length = 255)
    private String sailpointIdentityName;

    /** sys_id de l'incident ServiceNow (nullable si pas encore créé) */
    @Column(name = "servicenow_sys_id", length = 50)
    private String serviceNowSysId;

    /** Numéro d'incident ServiceNow lisible (ex: INC0012345) */
    @Column(name = "servicenow_number", length = 20)
    private String serviceNowNumber;

    /** URL directe vers l'incident ServiceNow */
    @Column(name = "servicenow_url", length = 500)
    private String serviceNowUrl;

    /** Lien vers le rapport d'audit CyberAudit7E (nullable si source != AUDIT) */
    @Column(name = "audit_report_id")
    private Long auditReportId;

    /** URL du site associé (si applicable) */
    @Column(name = "site_url", length = 500)
    private String siteUrl;

    // ── Assignation ──

    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    @Column(name = "assignment_group", length = 255)
    private String assignmentGroup;

    // ── Métadonnées ──

    /** Payload JSON brut reçu de SailPoint ou ServiceNow (pour debug) */
    @Column(name = "raw_payload", columnDefinition = "CLOB")
    private String rawPayload;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ── Lifecycle callbacks ──

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == TicketStatus.RESOLVED && resolvedAt == null) {
            resolvedAt = LocalDateTime.now();
        }
    }

    // ── Constructeurs ──

    public SecurityTicket() {}

    /**
     * Factory pour un ticket issu d'un audit CyberAudit7E.
     */
    public static SecurityTicket fromAudit(Long auditReportId, String siteUrl,
                                            double score, String details) {
        SecurityTicket ticket = new SecurityTicket();
        ticket.setSource(TicketSource.AUDIT);
        ticket.setCategory("accessibility");
        ticket.setSeverity(TicketSeverity.fromAuditScore(score));
        ticket.setTitle(String.format("Audit accessibilité — score %.0f%% — %s",
                score * 100, siteUrl));
        ticket.setDescription(details);
        ticket.setAuditReportId(auditReportId);
        ticket.setSiteUrl(siteUrl);
        return ticket;
    }

    /**
     * Factory pour un ticket issu d'une violation SailPoint.
     */
    public static SecurityTicket fromSailpoint(String eventType, String violationId,
                                                String identityId, String identityName,
                                                String riskTag, String description) {
        SecurityTicket ticket = new SecurityTicket();
        ticket.setSource(TicketSource.SAILPOINT);
        ticket.setSailpointEventType(eventType);
        ticket.setSailpointViolationId(violationId);
        ticket.setSailpointIdentityId(identityId);
        ticket.setSailpointIdentityName(identityName);
        ticket.setSeverity(TicketSeverity.fromSailpointRisk(riskTag));
        ticket.setCategory(mapSailpointEventToCategory(eventType));
        ticket.setTitle(String.format("[SailPoint] %s — %s",
                formatEventType(eventType), identityName != null ? identityName : violationId));
        ticket.setDescription(description);
        return ticket;
    }

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TicketSource getSource() { return source; }
    public void setSource(TicketSource source) { this.source = source; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public TicketSeverity getSeverity() { return severity; }
    public void setSeverity(TicketSeverity severity) { this.severity = severity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSailpointViolationId() { return sailpointViolationId; }
    public void setSailpointViolationId(String v) { this.sailpointViolationId = v; }
    public String getSailpointEventType() { return sailpointEventType; }
    public void setSailpointEventType(String t) { this.sailpointEventType = t; }
    public String getSailpointIdentityId() { return sailpointIdentityId; }
    public void setSailpointIdentityId(String id) { this.sailpointIdentityId = id; }
    public String getSailpointIdentityName() { return sailpointIdentityName; }
    public void setSailpointIdentityName(String n) { this.sailpointIdentityName = n; }
    public String getServiceNowSysId() { return serviceNowSysId; }
    public void setServiceNowSysId(String id) { this.serviceNowSysId = id; }
    public String getServiceNowNumber() { return serviceNowNumber; }
    public void setServiceNowNumber(String n) { this.serviceNowNumber = n; }
    public String getServiceNowUrl() { return serviceNowUrl; }
    public void setServiceNowUrl(String url) { this.serviceNowUrl = url; }
    public Long getAuditReportId() { return auditReportId; }
    public void setAuditReportId(Long id) { this.auditReportId = id; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String url) { this.siteUrl = url; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String to) { this.assignedTo = to; }
    public String getAssignmentGroup() { return assignmentGroup; }
    public void setAssignmentGroup(String g) { this.assignmentGroup = g; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String p) { this.rawPayload = p; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime r) { this.resolvedAt = r; }

    /**
     * Vérifie si le ticket est synchronisé avec ServiceNow.
     */
    public boolean isSyncedWithServiceNow() {
        return serviceNowSysId != null && !serviceNowSysId.isBlank();
    }

    // ── Helpers privés ──

    private static String mapSailpointEventToCategory(String eventType) {
        if (eventType == null) return "unknown";
        if (eventType.contains("policy-violation")) return "sod_violation";
        if (eventType.contains("identity-deleted")) return "orphan_account";
        if (eventType.contains("access-request")) return "access_violation";
        if (eventType.contains("account-aggregation")) return "account_anomaly";
        if (eventType.contains("certification")) return "certification_issue";
        return "identity_governance";
    }

    private static String formatEventType(String eventType) {
        if (eventType == null) return "Événement inconnu";
        return switch (eventType) {
            case "idn:policy-violation" -> "Violation de politique (SoD)";
            case "idn:identity-deleted" -> "Identité supprimée";
            case "idn:access-request-pre-approval" -> "Demande d'accès (pré-approbation)";
            case "idn:access-request-post-approval" -> "Demande d'accès (post-approbation)";
            case "idn:account-aggregation-completed" -> "Agrégation de comptes terminée";
            case "idn:certification-signed-off" -> "Certification signée";
            default -> eventType;
        };
    }

    @Override
    public String toString() {
        return String.format("SecurityTicket{id=%d, source=%s, severity=%s, status=%s, snow=%s, sp=%s}",
                id, source, severity, status, serviceNowNumber, sailpointViolationId);
    }
}
