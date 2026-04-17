package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * WCAG 2.1.1 + 2.4.1 — Navigation clavier et lien d'évitement.
 * M4 : vérifie les skip-nav links, tabindex négatifs, et focus traps potentiels.
 */
@Component
public class KeyboardNavRule implements AuditRule {

    @Override public String id() { return "WCAG-2.1.1"; }
    @Override public String description() { return "Navigation clavier et lien d'évitement"; }
    @Override public RuleCategory category() { return RuleCategory.WCAG; }
    @Override public int priority() { return 40; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        double score = 1.0;
        StringBuilder detail = new StringBuilder();

        // 1. Vérifier la présence d'un skip-nav link (lien d'évitement)
        Elements skipLinks = doc.select("a[href^=#]");
        boolean hasSkipNav = false;
        for (Element link : skipLinks) {
            String text = link.text().toLowerCase();
            String href = link.attr("href").toLowerCase();
            if (text.contains("contenu") || text.contains("content")
                    || text.contains("skip") || text.contains("aller au")
                    || href.contains("main") || href.contains("content")) {
                hasSkipNav = true;
                break;
            }
        }

        if (hasSkipNav) {
            detail.append("Lien d'évitement détecté");
        } else {
            score -= 0.3;
            detail.append("Lien d'évitement absent");
        }

        // 2. Vérifier les tabindex négatifs (éléments retirés du focus)
        Elements negativeTabindex = doc.select("[tabindex]");
        int negCount = 0;
        for (Element el : negativeTabindex) {
            try {
                int ti = Integer.parseInt(el.attr("tabindex"));
                if (ti < 0) negCount++;
            } catch (NumberFormatException ignored) {}
        }

        if (negCount > 5) {
            score -= 0.2;
            detail.append(String.format(" | %d tabindex négatif(s) (focus traps potentiels)", negCount));
        } else if (negCount > 0) {
            detail.append(String.format(" | %d tabindex négatif(s)", negCount));
        }

        // 3. Vérifier la présence de landmarks ARIA ou éléments sémantiques
        boolean hasMainLandmark = !doc.select("main, [role=main]").isEmpty();
        if (hasMainLandmark) {
            detail.append(" | <main> présent");
        } else {
            score -= 0.2;
            detail.append(" | <main> absent");
        }

        score = Math.max(0.0, Math.round(score * 100.0) / 100.0);
        return score >= 1.0
                ? RuleResultDto.success(id(), category(), detail.toString())
                : RuleResultDto.partial(id(), category(), score, detail.toString());
    }
}
