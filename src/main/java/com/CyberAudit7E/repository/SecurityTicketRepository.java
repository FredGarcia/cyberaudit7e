package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.SecurityTicket;
import com.cyberaudit7e.domain.enums.TicketSeverity;
import com.cyberaudit7e.domain.enums.TicketSource;
import com.cyberaudit7e.domain.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityTicketRepository extends JpaRepository<SecurityTicket, Long> {

    // ── Recherches par référence croisée ──

    Optional<SecurityTicket> findBySailpointViolationId(String violationId);

    Optional<SecurityTicket> findByServiceNowSysId(String sysId);

    Optional<SecurityTicket> findByServiceNowNumber(String number);

    Optional<SecurityTicket> findByAuditReportId(Long reportId);

    List<SecurityTicket> findBySailpointIdentityId(String identityId);

    // ── Filtres courants ──

    Page<SecurityTicket> findBySource(TicketSource source, Pageable pageable);

    Page<SecurityTicket> findByStatus(TicketStatus status, Pageable pageable);

    Page<SecurityTicket> findBySeverity(TicketSeverity severity, Pageable pageable);

    Page<SecurityTicket> findByStatusNot(TicketStatus status, Pageable pageable);

    List<SecurityTicket> findByStatusIn(List<TicketStatus> statuses);

    // ── Tickets ouverts (non résolus/fermés/annulés) ──

    @Query("SELECT t FROM SecurityTicket t WHERE t.status NOT IN ('RESOLVED','CLOSED','CANCELLED') ORDER BY t.severity ASC, t.createdAt DESC")
    List<SecurityTicket> findOpenTickets();

    @Query("SELECT t FROM SecurityTicket t WHERE t.status NOT IN ('RESOLVED','CLOSED','CANCELLED') ORDER BY t.severity ASC, t.createdAt DESC")
    Page<SecurityTicket> findOpenTickets(Pageable pageable);

    // ── Tickets non synchronisés avec ServiceNow ──

    @Query("SELECT t FROM SecurityTicket t WHERE t.serviceNowSysId IS NULL AND t.status NOT IN ('CLOSED','CANCELLED')")
    List<SecurityTicket> findUnsyncedWithServiceNow();

    // ── Comptages ──

    long countBySource(TicketSource source);

    long countByStatus(TicketStatus status);

    long countBySeverity(TicketSeverity severity);

    @Query("SELECT t.source, COUNT(t) FROM SecurityTicket t GROUP BY t.source")
    List<Object[]> countBySourceGroup();

    @Query("SELECT t.status, COUNT(t) FROM SecurityTicket t GROUP BY t.status")
    List<Object[]> countByStatusGroup();

    @Query("SELECT t.severity, COUNT(t) FROM SecurityTicket t GROUP BY t.severity")
    List<Object[]> countBySeverityGroup();

    // ── Recherche ──

    @Query("SELECT t FROM SecurityTicket t WHERE " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(t.sailpointIdentityName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(t.serviceNowNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(t.siteUrl) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<SecurityTicket> search(@Param("q") String query, Pageable pageable);
}
