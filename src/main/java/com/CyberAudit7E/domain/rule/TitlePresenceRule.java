package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * RGAA 8.5 — Chaque page web a-t-elle un titre de page pertinent ?
 * M4 : analyse réelle du DOM via Jsoup.
 */
@Component
public class TitlePresenceRule implements AuditRule {

    @Override public String id() { return "RGAA-8.5"; }
    @Override public String description() { return "Chaque page web a un titre de page pertinent"; }
    @Override public RuleCategory category() { return RuleCategory.RGAA; }
    @Override public int priority() { return 10; } // Priorité haute (structurel)

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        String title = doc.title();

        if (title == null || title.isBlank()) {
            return RuleResultDto.failure(id(), category(), "Titre <title> absent ou vide");
        }

        if (title.length() < 5) {
            return RuleResultDto.partial(id(), category(), 0.5,
                    String.format("Titre trop court (%d car.) : '%s'", title.length(), title));
        }

        // Vérifier que le titre n'est pas générique
        String lower = title.toLowerCase();
        if (lower.equals("home") || lower.equals("accueil") || lower.equals("untitled")) {
            return RuleResultDto.partial(id(), category(), 0.4,
                    String.format("Titre générique détecté : '%s'", title));
        }

        return RuleResultDto.success(id(), category(),
                String.format("Titre pertinent : '%s' (%d car.)", title, title.length()));
    }
}
