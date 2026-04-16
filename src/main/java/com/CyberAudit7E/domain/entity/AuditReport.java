package com.cyberaudit7e.domain.entity;

import com.cyberaudit7e.domain.enums.Phase7E;
import com.cyberaudit7e.dto.RuleResultDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rapport d'audit — résultat d'un cycle 7E complet sur un site.
 * Contient les scores pondérés (RGAA×0.5 + WCAG×0.3 + DSFR×0.2)
 * et le détail de chaque règle évaluée.
 *
 * M2 : POJO simple.
 * M3 : ajout des annotations JPA.
 */
public class AuditReport {

    private Long id;
    private Site site;
    private Double scoreRgaa;
    private Double scoreWcag;
    private Double scoreDsfr;
    private Double scoreGlobal;
    private Phase7E completedPhase;
    private List<RuleResultDto> ruleResults = new ArrayList<>();
    private LocalDateTime auditedAt;

    // ── Constructeurs ──

    public AuditReport() {
        this.auditedAt = LocalDateTime.now();
    }

    public AuditReport(Site site, Map<String, Double> scores, List<RuleResultDto> results) {
        this();
        this.site = site;
        this.scoreRgaa = scores.getOrDefault("rgaa", 0.0);
        this.scoreWcag = scores.getOrDefault("wcag", 0.0);
        this.scoreDsfr = scores.getOrDefault("dsfr", 0.0);
        this.scoreGlobal = scores.getOrDefault("global", 0.0);
        this.ruleResults = results;
        this.completedPhase = Phase7E.EQUILIBRER; // cycle complet
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

    public LocalDateTime getAuditedAt() { return auditedAt; }

    @Override
    public String toString() {
        return String.format("AuditReport{id=%d, site='%s', score=%.2f, phase=%s}",
                id, site != null ? site.getName() : "null", scoreGlobal, completedPhase);
    }
}
