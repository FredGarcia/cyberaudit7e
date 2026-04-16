package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.springframework.stereotype.Component;

/**
 * RGAA 1.1 — Chaque image porteuse d'information a-t-elle une alternative textuelle ?
 * POC : simulation.
 */
@Component
public class ImageAltRule implements AuditRule {

    @Override
    public String id() {
        return "RGAA-1.1";
    }

    @Override
    public String description() {
        return "Chaque image porteuse d'information a une alternative textuelle";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.RGAA;
    }

    @Override
    public RuleResultDto evaluate(String url) {
        // POC : simulation avec score partiel aléatoire reproductible
        double score = Math.abs(url.hashCode() % 100) / 100.0;
        return RuleResultDto.partial(id(), category(), score,
                String.format("Score alt-text simulé : %.0f%%", score * 100));
    }
}
