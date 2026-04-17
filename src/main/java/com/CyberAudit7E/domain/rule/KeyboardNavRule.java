package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.springframework.stereotype.Component;

/**
 * WCAG 2.1.1 — Toutes les fonctionnalités sont utilisables au clavier.
 * POC : simulation.
 */
@Component
public class KeyboardNavRule implements AuditRule {

    @Override
    public String id() {
        return "WCAG-2.1.1";
    }

    @Override
    public String description() {
        return "Toutes les fonctionnalités sont utilisables au clavier";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.WCAG;
    }

    @Override
    public RuleResultDto evaluate(String url) {
        boolean accessible = url != null && url.startsWith("https");
        return accessible
                ? RuleResultDto.success(id(), category(), "Navigation clavier OK (HTTPS détecté)")
                : RuleResultDto.partial(id(), category(), 0.5, "Site non-HTTPS — navigation clavier incertaine");
    }
}
