package com.CyberAudit7E.service.cycle;

import com.CyberAudit7E.dto.RuleResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 7E : ÉLABORER
 * Analyse les résultats des règles et génère un plan de remédiation.
 * En production : priorisation des violations, suggestions correctives.
 */
@Service
public class ElaborateService {

    private static final Logger log = LoggerFactory.getLogger(ElaborateService.class);

    /**
     * Élabore le plan de remédiation à partir des violations détectées.
     *
     * @param results Résultats bruts des règles
     * @return Liste filtrée des violations à corriger
     */
    public List<RuleResultDto> elaborate(List<RuleResultDto> results) {
        List<RuleResultDto> violations = results.stream()
                .filter(r -> !r.passed())
                .toList();

        log.info("[7E-ÉLABORER] {} violation(s) détectée(s) sur {} règle(s)",
                violations.size(), results.size());

        violations.forEach(v ->
                log.info("  ├─ [{}] {} — score: {}", v.ruleId(), v.detail(), v.score()));

        return violations;
    }
}
