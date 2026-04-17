package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * RGAA 11.1 — Chaque champ de formulaire a-t-il une étiquette ?
 * M4 NOUVEAU : vérifie que chaque input/select/textarea a un label associé
 * ou un attribut aria-label/aria-labelledby.
 */
@Component
public class FormLabelRule implements AuditRule {

    @Override public String id() { return "RGAA-11.1"; }
    @Override public String description() { return "Chaque champ de formulaire a une étiquette"; }
    @Override public RuleCategory category() { return RuleCategory.RGAA; }
    @Override public int priority() { return 60; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        Elements fields = doc.select("input:not([type=hidden]):not([type=submit]):not([type=button]):not([type=reset]):not([type=image]), select, textarea");

        if (fields.isEmpty()) {
            return RuleResultDto.success(id(), category(), "Aucun champ de formulaire détecté (N/A)");
        }

        int total = fields.size();
        int labeled = 0;

        for (Element field : fields) {
            if (hasLabel(doc, field)) {
                labeled++;
            }
        }

        double score = (double) labeled / total;
        String detail = String.format("%d champ(s) : %d avec étiquette, %d sans",
                total, labeled, total - labeled);

        if (labeled == total) {
            return RuleResultDto.success(id(), category(), detail);
        }

        return RuleResultDto.partial(id(), category(),
                Math.round(score * 100.0) / 100.0, detail);
    }

    private boolean hasLabel(Document doc, Element field) {
        // 1. Label explicite via for/id
        String id = field.attr("id");
        if (!id.isBlank()) {
            Element label = doc.selectFirst("label[for=" + id + "]");
            if (label != null && !label.text().isBlank()) return true;
        }

        // 2. Label implicite (field encapsulé dans un <label>)
        Element parent = field.parent();
        while (parent != null) {
            if ("label".equals(parent.tagName())) return true;
            parent = parent.parent();
        }

        // 3. ARIA alternatives
        if (!field.attr("aria-label").isBlank()) return true;
        if (!field.attr("aria-labelledby").isBlank()) return true;
        if (!field.attr("title").isBlank()) return true;
        if (!field.attr("placeholder").isBlank()) return false; // placeholder seul ≠ label

        return false;
    }
}
