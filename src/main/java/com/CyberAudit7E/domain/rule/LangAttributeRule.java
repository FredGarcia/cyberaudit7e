package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.springframework.stereotype.Component;

/**
 * RGAA 8.3 — La langue par défaut est-elle indiquée via l'attribut lang ?
 * POC : simulation.
 */
@Component
public class LangAttributeRule implements AuditRule {

    @Override
    public String id() {
        return "RGAA-8.3";
    }

    @Override
    public String description() {
        return "Langue par défaut indiquée dans l'élément HTML (attribut lang)";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.RGAA;
    }

    @Override
    public RuleResultDto evaluate(String url) {
        // POC : simulation — on considère que les sites .fr ont un lang
        boolean hasLang = url != null && url.contains(".fr");
        return hasLang
                ? RuleResultDto.success(id(), category(), "Attribut lang='fr' détecté")
                : RuleResultDto.partial(id(), category(), 0.3, "Attribut lang absent ou non-fr");
    }
}
