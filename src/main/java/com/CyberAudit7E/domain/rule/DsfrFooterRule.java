package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * DSFR — Pied de page conforme avec mentions obligatoires.
 * M4 : vérifie la structure DSFR du footer et la présence des liens obligatoires
 * (mentions légales, accessibilité, plan du site, données personnelles).
 */
@Component
public class DsfrFooterRule implements AuditRule {

    @Override public String id() { return "DSFR-FTR-01"; }
    @Override public String description() { return "Pied de page conforme DSFR avec mentions obligatoires"; }
    @Override public RuleCategory category() { return RuleCategory.DSFR; }
    @Override public int priority() { return 25; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        double score = 0.0;
        StringBuilder detail = new StringBuilder();
        int checks = 5;
        int passed = 0;

        // 1. Classe fr-footer
        Elements frFooter = doc.select(".fr-footer, footer[class*=fr-footer]");
        if (!frFooter.isEmpty()) { passed++; detail.append("fr-footer "); }

        // 2. Mentions légales
        String bodyText = doc.body() != null ? doc.body().text().toLowerCase() : "";
        Elements footerLinks = doc.select("footer a, .fr-footer a");

        boolean hasMentions = footerLinks.stream()
                .anyMatch(a -> {
                    String t = a.text().toLowerCase();
                    return t.contains("mentions légales") || t.contains("mentions legales");
                });
        if (hasMentions) { passed++; detail.append("mentions-légales "); }

        // 3. Déclaration d'accessibilité
        boolean hasA11y = footerLinks.stream()
                .anyMatch(a -> {
                    String t = a.text().toLowerCase();
                    return t.contains("accessibilité") || t.contains("accessibilite")
                            || t.contains("a11y");
                });
        if (hasA11y) { passed++; detail.append("accessibilité "); }

        // 4. Plan du site
        boolean hasSitemap = footerLinks.stream()
                .anyMatch(a -> {
                    String t = a.text().toLowerCase();
                    return t.contains("plan du site") || t.contains("sitemap");
                });
        if (hasSitemap) { passed++; detail.append("plan-du-site "); }

        // 5. Données personnelles / RGPD
        boolean hasRgpd = footerLinks.stream()
                .anyMatch(a -> {
                    String t = a.text().toLowerCase();
                    return t.contains("données personnelles") || t.contains("donnees personnelles")
                            || t.contains("vie privée") || t.contains("rgpd")
                            || t.contains("cookies");
                });
        if (hasRgpd) { passed++; detail.append("RGPD "); }

        score = (double) passed / checks;
        String msg = String.format("Footer DSFR : %d/%d critères [%s]",
                passed, checks, detail.toString().trim());

        if (passed == checks) {
            return RuleResultDto.success(id(), category(), msg);
        } else if (passed == 0) {
            return RuleResultDto.failure(id(), category(),
                    "Aucun composant DSFR détecté dans le pied de page");
        }

        return RuleResultDto.partial(id(), category(),
                Math.round(score * 100.0) / 100.0, msg);
    }
}
