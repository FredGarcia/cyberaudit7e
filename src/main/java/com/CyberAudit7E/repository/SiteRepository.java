package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.Site;
import com.cyberaudit7e.domain.enums.Phase7E;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour les Sites.
 *
 * M3 : l'implémentation ConcurrentHashMap de M2 est remplacée par
 * une interface JpaRepository. Spring Data génère automatiquement
 * l'implémentation à partir des noms de méthodes (Query Methods).
 *
 * Comparaison avec les autres stacks :
 * - Django : Site.objects.filter(url=url)
 * - Laravel : Site::where('url', $url)->first()
 * - Spring : findByUrl(String url) → Spring génère le SQL
 *
 * JpaRepository hérite de CrudRepository + PagingAndSortingRepository,
 * offrant save(), findById(), findAll(), delete(), count(), etc.
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    /**
     * Trouve un site par son URL exacte.
     * SQL généré : SELECT * FROM sites WHERE url = ?
     */
    Optional<Site> findByUrl(String url);

    /**
     * Vérifie l'existence d'un site par URL (optimisé, pas de SELECT *).
     * SQL généré : SELECT COUNT(*) > 0 FROM sites WHERE url = ?
     */
    boolean existsByUrl(String url);

    /**
     * Liste les sites par phase 7E courante.
     * SQL généré : SELECT * FROM sites WHERE current_phase = ?
     */
    List<Site> findByCurrentPhase(Phase7E phase);

    /**
     * Liste les sites triés par date de création décroissante.
     */
    List<Site> findAllByOrderByCreatedAtDesc();

    /**
     * Recherche par nom (LIKE, insensible à la casse).
     * SQL généré : SELECT * FROM sites WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?,
     * '%'))
     */
    List<Site> findByNameContainingIgnoreCase(String name);

    /**
     * Compte les sites par phase (query custom JPQL).
     */
    @Query("SELECT s.currentPhase, COUNT(s) FROM Site s GROUP BY s.currentPhase")
    List<Object[]> countByPhase();
}
