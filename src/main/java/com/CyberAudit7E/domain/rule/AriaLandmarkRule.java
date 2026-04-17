package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * WCAG 1.3.1 — Structure sémantique : landmarks ARIA.
 * M4 NOUVEAU : vérifie la présence des landmarks obligatoires
 * (header/banner, nav/navigation, main, footer/contentinfo).
 */
@Component
public class AriaLandmarkRule implements AuditRule {

    @Override public String id() { return "WCAG-1.3.1"; }
    @Override public String description() { return "Landmarks ARIA et structure sémantique"; }
    @Override public RuleCategory category() { return RuleCategory.WCAG; }
    @Override public int priority() { return 20; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        int found = 0;
        int total = 4;
        StringBuilder detail = new StringBuilder();

        // 1. Banner (header)
        Elements banner = doc.select("header, [role=banner]");
        if (!banner.isEmpty()) { found++; detail.append("banner "); }

        // 2. Navigation
        Elements nav = doc.select("nav, [role=navigation]");
        if (!nav.isEmpty()) { found++; detail.append("nav "); }

        // 3. Main
        Elements main = doc.select("main, [role=main]");
        if (!main.isEmpty()) { found++; detail.append("main "); }

        // 4. Contentinfo (footer)
        Elements footer = doc.select("footer, [role=contentinfo]");
        if (!footer.isEmpty()) { found++; detail.append("footer "); }

        double score = (double) found / total;
        String msg = String.format("Landmarks : %d/%d détectés [%s]",
                found, total, detail.toString().trim());

        if (found == total) {
            return RuleResultDto.success(id(), category(), msg);
        }

        return RuleResultDto.partial(id(), category(),
                Math.round(score * 100.0) / 100.0, msg);
    }
}
