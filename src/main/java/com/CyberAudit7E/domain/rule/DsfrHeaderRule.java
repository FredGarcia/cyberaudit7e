package com.CyberAudit7E.domain.rule;

import com.CyberAudit7E.domain.enums.RuleCategory;
import com.CyberAudit7E.dto.RuleResultDto;
import org.springframework.stereotype.Component;

/**
 * DSFR — Présence de l'en-tête conforme au Design System de l'État.
 * Vérifie la conformité structurelle de l'en-tête (header DSFR).
 * POC : simulation.
 */
@Component
public class DsfrHeaderRule implements AuditRule {

    @Override
    public String id() {
        return "DSFR-HDR-01";
    }

    @Override
    public String description() {
        return "En-tête conforme au Design System de l'État (DSFR)";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.DSFR;
    }

    @Override
    public RuleResultDto evaluate(String url) {
        // POC : les sites .gouv.fr sont considérés conformes
        boolean isGouv = url != null && url.contains(".gouv.fr");
        return isGouv
                ? RuleResultDto.success(id(), category(), "En-tête DSFR détecté (site .gouv.fr)")
                : RuleResultDto.failure(id(), category(), "En-tête DSFR non détecté");
    }
}
