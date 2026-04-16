package com.CyberAudit7E.domain.rule;

import com.CyberAudit7E.domain.enums.RuleCategory;
import com.CyberAudit7E.dto.RuleResultDto;
import org.springframework.stereotype.Component;

/**
 * DSFR — Présence du pied de page conforme (mentions légales, plan du site, accessibilité).
 * POC : simulation.
 */
@Component
public class DsfrFooterRule implements AuditRule {

    @Override
    public String id() {
        return "DSFR-FTR-01";
    }

    @Override
    public String description() {
        return "Pied de page conforme DSFR avec mentions obligatoires";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.DSFR;
    }

    @Override
    public RuleResultDto evaluate(String url) {
        boolean isGouv = url != null && url.contains(".gouv.fr");
        double score = isGouv ? 0.9 : 0.2;
        return RuleResultDto.partial(id(), category(), score,
                isGouv ? "Footer DSFR partiellement conforme"
                        : "Footer DSFR non détecté — mentions obligatoires absentes");
    }
}
