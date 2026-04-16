package com.CyberAudit7E.service.cycle;

import com.CyberAudit7E.dto.RuleResultDto;
import com.CyberAudit7E.service.ScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Phase 7E : EXAMINER
 * Calcule le score pondéré composite via ScoringService.
 * Formule : score = RGAA×0.5 + WCAG×0.3 + DSFR×0.2
 */
@Service
public class ExamineService {

    private static final Logger log = LoggerFactory.getLogger(ExamineService.class);

    private final ScoringService scoringService;

    public ExamineService(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    /**
     * Examine les résultats et produit les scores.
     *
     * @param results Résultats des règles
     * @return Scores par catégorie + global
     */
    public Map<String, Double> examine(List<RuleResultDto> results) {
        log.info("[7E-EXAMINER] Calcul du scoring pondéré sur {} résultat(s)", results.size());
        Map<String, Double> scores = scoringService.computeScores(results);
        log.info("[7E-EXAMINER] Score global : {}/1.00", scores.get("global"));
        return scores;
    }
}
