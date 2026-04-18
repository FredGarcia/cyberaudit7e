package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.AuditReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour les rapports d'audit.
 * M6 : ajout des méthodes paginées (Page + Pageable).
 */
@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, Long> {

    // ── Requêtes non-paginées (M3/M4/M5) ──

    List<AuditReport> findBySiteIdOrderByAuditedAtDesc(Long siteId);

    Optional<AuditReport> findFirstBySiteIdOrderByAuditedAtDesc(Long siteId);

    List<AuditReport> findByScoreGlobalGreaterThanEqual(Double threshold);

    List<AuditReport> findByScoreGlobalLessThan(Double threshold);

    List<AuditReport> findByAuditedAtBetweenOrderByAuditedAtDesc(
            LocalDateTime start, LocalDateTime end);

    long countBySiteId(Long siteId);

    @Query("SELECT AVG(r.scoreGlobal) FROM AuditReport r")
    Double averageGlobalScore();

    @Query("SELECT AVG(r.scoreGlobal) FROM AuditReport r WHERE r.site.id = :siteId")
    Double averageGlobalScoreBySite(@Param("siteId") Long siteId);

    List<AuditReport> findByTrend(String trend);

    // ── Requêtes paginées (M6 NOUVEAU) ──

    /**
     * Tous les rapports, paginés et triés.
     * Exemple : GET /api/audits?page=0&size=10&sort=auditedAt,desc
     */
    Page<AuditReport> findAll(Pageable pageable);

    /**
     * Rapports d'un site, paginés.
     */
    Page<AuditReport> findBySiteId(Long siteId, Pageable pageable);

    /**
     * Rapports sous un seuil de score, paginés.
     */
    Page<AuditReport> findByScoreGlobalLessThan(Double threshold, Pageable pageable);

    /**
     * Rapports au-dessus d'un seuil, paginés.
     */
    Page<AuditReport> findByScoreGlobalGreaterThanEqual(Double threshold, Pageable pageable);

    /**
     * Rapports par tendance, paginés.
     */
    Page<AuditReport> findByTrend(String trend, Pageable pageable);

    /**
     * Recherche full-text sur les résultats JSON (JPQL LIKE).
     */
    @Query("SELECT r FROM AuditReport r WHERE r.site.name LIKE %:query% OR r.site.url LIKE %:query%")
    Page<AuditReport> searchByQuery(@Param("query") String query, Pageable pageable);
}
