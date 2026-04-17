package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * WCAG 2.4.4 — L'objectif de chaque lien est-il déterminable par son intitulé seul ?
 * M4 NOUVEAU : détecte les liens non-descriptifs ("cliquez ici", "en savoir plus", etc.)
 */
@Component
public class LinkPurposeRule implements AuditRule {

    private static final Set<String> VAGUE_TEXTS = Set.of(
            "cliquez ici", "cliquer ici", "ici", "lire la suite",
            "en savoir plus", "plus", "voir", "lien", "click here",
            "here", "read more", "more", "link", "learn more"
    );

    @Override public String id() { return "WCAG-2.4.4"; }
    @Override public String description() { return "Intitulé des liens descriptif et compréhensible"; }
    @Override public RuleCategory category() { return RuleCategory.WCAG; }
    @Override public int priority() { return 70; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        Elements links = doc.select("a[href]");

        if (links.isEmpty()) {
            return RuleResultDto.success(id(), category(), "Aucun lien détecté (N/A)");
        }

        int total = links.size();
        int vague = 0;
        int empty = 0;

        for (Element link : links) {
            String text = link.text().trim().toLowerCase();
            String ariaLabel = link.attr("aria-label").trim();
            String title = link.attr("title").trim();

            // Texte effectif : text > aria-label > title
            String effective = !text.isBlank() ? text
                    : !ariaLabel.isBlank() ? ariaLabel.toLowerCase()
                    : title.toLowerCase();

            if (effective.isBlank()) {
                // Vérifier si c'est un lien-image avec alt
                Element img = link.selectFirst("img[alt]");
                if (img == null || img.attr("alt").isBlank()) {
                    empty++;
                }
            } else if (VAGUE_TEXTS.contains(effective)) {
                vague++;
            }
        }

        int issues = vague + empty;
        double score = 1.0 - ((double) issues / total);
        score = Math.max(0.0, Math.round(score * 100.0) / 100.0);

        String detail = String.format("%d lien(s) : %d vague(s), %d vide(s), %d OK",
                total, vague, empty, total - issues);

        if (issues == 0) {
            return RuleResultDto.success(id(), category(), detail);
        }

        return RuleResultDto.partial(id(), category(), score, detail);
    }
}
