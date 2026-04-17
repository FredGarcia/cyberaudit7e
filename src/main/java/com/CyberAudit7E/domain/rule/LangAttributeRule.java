package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * RGAA 8.3 — La langue par défaut est-elle indiquée via l'attribut lang ?
 * M4 : vérifie réellement la présence et la validité de l'attribut lang sur <html>.
 */
@Component
public class LangAttributeRule implements AuditRule {

    @Override public String id() { return "RGAA-8.3"; }
    @Override public String description() { return "Langue par défaut indiquée dans l'élément HTML"; }
    @Override public RuleCategory category() { return RuleCategory.RGAA; }
    @Override public int priority() { return 10; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        Element html = doc.selectFirst("html");

        if (html == null) {
            return RuleResultDto.failure(id(), category(), "Élément <html> introuvable");
        }

        String lang = html.attr("lang");
        String xmlLang = html.attr("xml:lang");
        String effectiveLang = !lang.isBlank() ? lang : xmlLang;

        if (effectiveLang.isBlank()) {
            return RuleResultDto.failure(id(), category(),
                    "Attribut lang absent sur <html>");
        }

        // Vérifier format BCP 47 basique (ex: "fr", "fr-FR", "en-US")
        if (!effectiveLang.matches("^[a-zA-Z]{2,3}(-[a-zA-Z]{2,4})?$")) {
            return RuleResultDto.partial(id(), category(), 0.5,
                    String.format("Attribut lang mal formé : '%s'", effectiveLang));
        }

        return RuleResultDto.success(id(), category(),
                String.format("Langue déclarée : lang='%s'", effectiveLang));
    }
}
