package com.CyberAudit7E.domain.rule;

import com.CyberAudit7E.domain.enums.RuleCategory;
import com.CyberAudit7E.dto.RuleResultDto;
import org.springframework.stereotype.Component;

/**
 * WCAG 1.4.3 — Contraste minimum (AA) : ratio 4.5:1 pour le texte normal.
 * POC : simulation.
 */
@Component
public class ContrastRule implements AuditRule {

    @Override
    public String id() {
        return "WCAG-1.4.3";
    }

    @Override
    public String description() {
        return "Contraste minimum de 4.5:1 pour le texte normal (niveau AA)";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.WCAG;
    }

    @Override
    public RuleResultDto evaluate(String url) {
        double score = 0.7 + (Math.abs(url.hashCode() % 30)) / 100.0;
        score = Math.min(score, 1.0);
        return RuleResultDto.partial(id(), category(), score,
                String.format("Ratio de contraste simulé : %.2f:1", score * 7));
    }
}
