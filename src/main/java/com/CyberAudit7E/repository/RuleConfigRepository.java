package com.cyberaudit7e.repository;

import com.cyberaudit7e.domain.entity.RuleConfig;
import com.cyberaudit7e.domain.enums.RuleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour la configuration dynamique des poids de scoring.
 * Utilisé par ScoringService pour charger les poids en BDD
 * au lieu des valeurs codées en dur.
 */
@Repository
public interface RuleConfigRepository extends JpaRepository<RuleConfig, Long> {

    Optional<RuleConfig> findByCategory(RuleCategory category);

    List<RuleConfig> findByEnabledTrue();
}
