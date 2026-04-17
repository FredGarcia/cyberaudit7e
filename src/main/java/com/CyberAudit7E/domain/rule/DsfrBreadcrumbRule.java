package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * DSFR — Fil d'Ariane conforme.
 * M4 NOUVEAU : vérifie la présence et la conformité du breadcrumb DSFR.
 * Le DSFR impose le composant fr-breadcrumb avec nav[aria-label].
 */
@Component
public class DsfrBreadcrumbRule implements AuditRule {

    @Override public String id() { return "DSFR-BRD-01"; }
    @Override public String description() { return "Fil d'Ariane conforme au DSFR"; }
    @Override public RuleCategory category() { return RuleCategory.DSFR; }
    @Override public int priority() { return 30; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();

        // Chercher le breadcrumb DSFR
        Elements dsfrBreadcrumb = doc.select(".fr-breadcrumb, [class*=fr-breadcrumb]");
        Elements genericBreadcrumb = doc.select(
                "nav[aria-label*=Ariane], nav[aria-label*=breadcrumb], " +
                "[role=navigation][aria-label*=Ariane], .breadcrumb, [class*=breadcrumb]");

        if (!dsfrBreadcrumb.isEmpty()) {
            // Vérifier la structure sémantique
            boolean hasNav = !dsfrBreadcrumb.select("nav").isEmpty()
                    || dsfrBreadcrumb.first().tagName().equals("nav");
            boolean hasAriaLabel = !dsfrBreadcrumb.select("[aria-label]").isEmpty();
            boolean hasList = !dsfrBreadcrumb.select("ol, ul").isEmpty();

            int criteria = 0;
            if (hasNav) criteria++;
            if (hasAriaLabel) criteria++;
            if (hasList) criteria++;

            if (criteria == 3) {
                return RuleResultDto.success(id(), category(),
                        "Fil d'Ariane DSFR complet (nav + aria-label + liste)");
            }
            return RuleResultDto.partial(id(), category(),
                    Math.round((double) criteria / 3 * 100.0) / 100.0,
                    String.format("Fil d'Ariane DSFR partiel : %d/3 critères", criteria));

        } else if (!genericBreadcrumb.isEmpty()) {
            return RuleResultDto.partial(id(), category(), 0.5,
                    "Fil d'Ariane générique détecté — non conforme DSFR");
        }

        return RuleResultDto.failure(id(), category(),
                "Aucun fil d'Ariane détecté");
    }
}
