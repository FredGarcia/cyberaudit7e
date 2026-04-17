package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * RGAA 9.1 — La hiérarchie des titres est-elle pertinente ?
 * M4 NOUVEAU : vérifie la structure h1→h6 (pas de saut de niveau).
 */
@Component
public class HeadingStructureRule implements AuditRule {

    @Override public String id() { return "RGAA-9.1"; }
    @Override public String description() { return "Hiérarchie des titres h1-h6 cohérente"; }
    @Override public RuleCategory category() { return RuleCategory.RGAA; }
    @Override public int priority() { return 30; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        Elements headings = doc.select("h1, h2, h3, h4, h5, h6");

        if (headings.isEmpty()) {
            return RuleResultDto.partial(id(), category(), 0.3,
                    "Aucun titre h1-h6 détecté — structuration manquante");
        }

        // Vérifier la présence d'un h1 unique
        Elements h1s = doc.select("h1");
        boolean hasUniqueH1 = h1s.size() == 1;

        // Vérifier qu'il n'y a pas de saut de niveau (h1 → h3 sans h2)
        int skips = 0;
        int previousLevel = 0;
        for (Element h : headings) {
            int level = Integer.parseInt(h.tagName().substring(1));
            if (previousLevel > 0 && level > previousLevel + 1) {
                skips++;
            }
            previousLevel = level;
        }

        // Calcul du score
        double score = 1.0;
        StringBuilder detail = new StringBuilder();
        detail.append(String.format("%d titre(s) détecté(s)", headings.size()));

        if (!hasUniqueH1) {
            score -= 0.3;
            detail.append(String.format(" | h1: %d (attendu: 1)", h1s.size()));
        } else {
            detail.append(" | h1 unique OK");
        }

        if (skips > 0) {
            score -= 0.15 * skips;
            detail.append(String.format(" | %d saut(s) de niveau", skips));
        } else {
            detail.append(" | hiérarchie continue");
        }

        score = Math.max(0.0, score);
        return score >= 1.0
                ? RuleResultDto.success(id(), category(), detail.toString())
                : RuleResultDto.partial(id(), category(),
                    Math.round(score * 100.0) / 100.0, detail.toString());
    }
}
