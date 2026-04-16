package com.CyberAudit7E.domain.rule;

import com.CyberAudit7E.domain.enums.RuleCategory;
import com.CyberAudit7E.dto.RuleResultDto;
import org.springframework.stereotype.Component;

/**
 * RGAA 8.5 — Chaque page web a-t-elle un titre de page ?
 * POC : simulation basée sur la validité de l'URL.
 */
@Component
public class TitlePresenceRule implements AuditRule {

    @Override
    public String id() {
        return "RGAA-8.5";
    }

    @Override
    public String description() {
        return "Chaque page web a un titre de page pertinent";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.RGAA;
    }

    @Override
    public RuleResultDto evaluate(String url) {
        // POC : on simule la vérification
        // Production : Jsoup.connect(url).get().title()
        boolean hasTitle = url != null && !url.isBlank();
        return hasTitle
                ? RuleResultDto.success(id(), category(), "Titre de page présent")
                : RuleResultDto.failure(id(), category(), "Titre de page manquant");
    }
}
