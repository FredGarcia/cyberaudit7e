package com.cyberaudit7e.dto.integration;

import com.cyberaudit7e.domain.entity.SecurityTicket;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour un SecurityTicket.
 * Expose toutes les données sans les champs internes JPA.
 */
public record TicketDto(
        Long id,
        String title,
        String description,
        String source,
        String status,
        String severity,
        String category,
        // Références croisées
        String sailpointViolationId,
        String sailpointEventType,
        String sailpointIdentityId,
        String sailpointIdentityName,
        String serviceNowSysId,
        String serviceNowNumber,
        String serviceNowUrl,
        Long auditReportId,
        String siteUrl,
        // Assignation
        String assignedTo,
        String assignmentGroup,
        // Sync status
        boolean syncedWithServiceNow,
        // Dates
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {

    public static TicketDto from(SecurityTicket t) {
        return new TicketDto(
                t.getId(), t.getTitle(), t.getDescription(),
                t.getSource().name(), t.getStatus().name(), t.getSeverity().name(),
                t.getCategory(),
                t.getSailpointViolationId(), t.getSailpointEventType(),
                t.getSailpointIdentityId(), t.getSailpointIdentityName(),
                t.getServiceNowSysId(), t.getServiceNowNumber(), t.getServiceNowUrl(),
                t.getAuditReportId(), t.getSiteUrl(),
                t.getAssignedTo(), t.getAssignmentGroup(),
                t.isSyncedWithServiceNow(),
                t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt()
        );
    }
}
