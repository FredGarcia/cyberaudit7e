package com.cyberaudit7e.domain.entity;

import com.cyberaudit7e.domain.enums.Phase7E;
import com.cyberaudit7e.dto.RuleResultDto;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entité JPA AuditReport — résultat d'un cycle 7E complet.
 *
 * M3 : POJO M2 transformé en entité JPA.
 * Changements vs M2 :
 * - @Entity, @Table, @ManyToOne avec LAZY fetch
 * - Scores stockés en colonnes DOUBLE
 * - RuleResults stockés en JSON (TEXT) via @Convert
 * - @PrePersist pour le timestamp d'audit
 */
@Entity
@Table(name = "audit_reports")
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "score_rgaa")
    private Double scoreRgaa;

    @Column(name = "score_wcag")
    private Double scoreWcag;

    @Column(name = "score_dsfr")
    private Double scoreDsfr;

    @Column(name = "score_global")
    private Double scoreGlobal;

    @Enumerated(EnumType.STRING)
    @Column(name = "completed_phase", length = 20)
    private Phase7E completedPhase;

    /**
     * Détails des résultats stockés en JSON dans une colonne TEXT.
     * Le converter RuleResultListConverter gère la sérialisation.
     */
    @Column(name = "results_json", columnDefinition = "CLOB")
    @Convert(converter = RuleResultListConverter.class)
    private List<RuleResultDto> ruleResults = new ArrayList<>();

    /** Tendance détectée par EvolveService (FIRST, UP, DOWN, STABLE) */
    @Column(name = "trend", length = 10)
    private String trend;

    @Column(name = "audited_at", updatable = false)
    private LocalDateTime auditedAt;

    // ── Lifecycle ──

    @PrePersist
    protected void onCreate() {
        auditedAt = LocalDateTime.now();
    }

    // ── Constructeurs ──

    public AuditReport() {
    }

    public AuditReport(Site site, Map<String, Double> scores, List<RuleResultDto> results) {
        this.site = site;
        this.scoreRgaa = scores.getOrDefault("rgaa", 0.0);
        this.scoreWcag = scores.getOrDefault("wcag", 0.0);
        this.scoreDsfr = scores.getOrDefault("dsfr", 0.0);
        this.scoreGlobal = scores.getOrDefault("global", 0.0);
        this.ruleResults = results;
        this.completedPhase = Phase7E.EQUILIBRER;
    }

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    public Double getScoreRgaa() { return scoreRgaa; }
    public void setScoreRgaa(Double scoreRgaa) { this.scoreRgaa = scoreRgaa; }

    public Double getScoreWcag() { return scoreWcag; }
    public void setScoreWcag(Double scoreWcag) { this.scoreWcag = scoreWcag; }

    public Double getScoreDsfr() { return scoreDsfr; }
    public void setScoreDsfr(Double scoreDsfr) { this.scoreDsfr = scoreDsfr; }

    public Double getScoreGlobal() { return scoreGlobal; }
    public void setScoreGlobal(Double scoreGlobal) { this.scoreGlobal = scoreGlobal; }

    public Phase7E getCompletedPhase() { return completedPhase; }
    public void setCompletedPhase(Phase7E completedPhase) { this.completedPhase = completedPhase; }

    public List<RuleResultDto> getRuleResults() { return ruleResults; }
    public void setRuleResults(List<RuleResultDto> ruleResults) { this.ruleResults = ruleResults; }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public LocalDateTime getAuditedAt() { return auditedAt; }

    @Override
    public String toString() {
        return String.format("AuditReport{id=%d, site='%s', score=%.2f, phase=%s, trend=%s}",
                id, site != null ? site.getName() : "null",
                scoreGlobal != null ? scoreGlobal : 0.0,
                completedPhase, trend);
    }
}
