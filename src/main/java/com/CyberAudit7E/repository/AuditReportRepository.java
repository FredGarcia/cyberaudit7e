package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.AuditReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour les rapports d'audit.
 *
 * M3 : remplace le ConcurrentHashMap de M2.
 * Les Query Methods de Spring Data dérivent le SQL du nom de la méthode.
 *
 * Convention de nommage :
 *   findBy[Champ][Condition]OrderBy[Champ][Direction]
 *   → Spring parse le nom et génère la requête SQL/JPQL correspondante.
 */
@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, Long> {

    /**
     * Historique des audits d'un site (plus récent en premier).
     * SQL généré : SELECT * FROM audit_reports WHERE site_id = ? ORDER BY audited_at DESC
     */
    List<AuditReport> findBySiteIdOrderByAuditedAtDesc(Long siteId);

    /**
     * Dernier audit d'un site.
     * Spring Data : "First" limite à 1 résultat.
     */
    Optional<AuditReport> findFirstBySiteIdOrderByAuditedAtDesc(Long siteId);

    /**
     * Tous les rapports avec un score global supérieur au seuil.
     */
    List<AuditReport> findByScoreGlobalGreaterThanEqual(Double threshold);

    /**
     * Tous les rapports avec un score global inférieur au seuil (alertes).
     */
    List<AuditReport> findByScoreGlobalLessThan(Double threshold);

    /**
     * Rapports d'audit dans une plage de dates.
     */
    List<AuditReport> findByAuditedAtBetweenOrderByAuditedAtDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * Nombre d'audits pour un site donné.
     */
    long countBySiteId(Long siteId);

    /**
     * Score moyen global (JPQL custom).
     */
    @Query("SELECT AVG(r.scoreGlobal) FROM AuditReport r")
    Double averageGlobalScore();

    /**
     * Score moyen par site (JPQL custom).
     */
    @Query("SELECT AVG(r.scoreGlobal) FROM AuditReport r WHERE r.site.id = :siteId")
    Double averageGlobalScoreBySite(@Param("siteId") Long siteId);

    /**
     * Rapports avec tendance spécifique.
     */
    List<AuditReport> findByTrend(String trend);
}
