package com.CyberAudit7E.service;

import com.CyberAudit7E.domain.enums.RuleCategory;
import com.CyberAudit7E.dto.RuleResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de scoring pondéré.
 * Formule AuditAccess : score = RGAA×0.5 + WCAG×0.3 + DSFR×0.2
 *
 * Les poids sont lus depuis RuleCategory.getDefaultWeight()
 * pour permettre un ajustement dynamique (phase Équilibrer).
 */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    /**
     * Calcule les scores par catégorie et le score global composite.
     *
     * @param results Liste des résultats de règles
     * @return Map avec clés : "rgaa", "wcag", "dsfr", "global"
     */
    public Map<String, Double> computeScores(List<RuleResultDto> results) {
        Map<RuleCategory, DoubleSummaryStatistics> stats = results.stream()
                .collect(Collectors.groupingBy(
                        RuleResultDto::category,
                        Collectors.summarizingDouble(RuleResultDto::score)
                ));

        double scoreRgaa = average(stats, RuleCategory.RGAA);
        double scoreWcag = average(stats, RuleCategory.WCAG);
        double scoreDsfr = average(stats, RuleCategory.DSFR);

        double global = scoreRgaa * RuleCategory.RGAA.getDefaultWeight()
                       + scoreWcag * RuleCategory.WCAG.getDefaultWeight()
                       + scoreDsfr * RuleCategory.DSFR.getDefaultWeight();

        log.info("[7E-EXAMINER] Scores — RGAA: {}, WCAG: {}, DSFR: {}, Global: {}",
                round(scoreRgaa), round(scoreWcag), round(scoreDsfr), round(global));

        return Map.of(
                "rgaa", round(scoreRgaa),
                "wcag", round(scoreWcag),
                "dsfr", round(scoreDsfr),
                "global", round(global)
        );
    }

    private double average(Map<RuleCategory, DoubleSummaryStatistics> stats, RuleCategory cat) {
        return stats.containsKey(cat) ? stats.get(cat).getAverage() : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
