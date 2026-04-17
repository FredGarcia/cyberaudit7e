package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * RGAA 1.1 — Chaque image porteuse d'information a-t-elle une alternative textuelle ?
 * M4 : analyse réelle des balises <img> dans le DOM.
 */
@Component
public class ImageAltRule implements AuditRule {

    @Override public String id() { return "RGAA-1.1"; }
    @Override public String description() { return "Chaque image a une alternative textuelle"; }
    @Override public RuleCategory category() { return RuleCategory.RGAA; }
    @Override public int priority() { return 50; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        Elements images = doc.select("img");

        if (images.isEmpty()) {
            return RuleResultDto.success(id(), category(), "Aucune image détectée (N/A)");
        }

        int total = images.size();
        int withAlt = 0;
        int emptyAlt = 0;
        int missingAlt = 0;

        for (Element img : images) {
            if (!img.hasAttr("alt")) {
                missingAlt++;
            } else if (img.attr("alt").isBlank()) {
                // alt="" est valide pour les images décoratives (RGAA)
                emptyAlt++;
                withAlt++;
            } else {
                withAlt++;
            }
        }

        double score = (double) withAlt / total;

        String detail = String.format(
                "%d image(s) : %d avec alt, %d alt vide (décoratif), %d sans alt",
                total, withAlt - emptyAlt, emptyAlt, missingAlt);

        if (missingAlt == 0) {
            return RuleResultDto.success(id(), category(), detail);
        }

        return RuleResultDto.partial(id(), category(), Math.round(score * 100.0) / 100.0, detail);
    }
}
