package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * DSFR — En-tête conforme au Design System de l'État.
 * M4 : vérifie les classes DSFR spécifiques et la structure de l'en-tête.
 * Le DSFR définit des classes CSS standardisées (fr-header, fr-logo, etc.)
 */
@Component
public class DsfrHeaderRule implements AuditRule {

    @Override public String id() { return "DSFR-HDR-01"; }
    @Override public String description() { return "En-tête conforme au Design System de l'État"; }
    @Override public RuleCategory category() { return RuleCategory.DSFR; }
    @Override public int priority() { return 20; }

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

        // 1. Classe fr-header
        Elements frHeader = doc.select(".fr-header, [class*=fr-header]");
        if (!frHeader.isEmpty()) { passed++; detail.append("fr-header "); }

        // 2. Logo République (fr-logo ou Marianne)
        Elements frLogo = doc.select(".fr-logo, [class*=fr-logo], img[alt*=Marianne], img[alt*=République]");
        if (!frLogo.isEmpty()) { passed++; detail.append("fr-logo "); }

        // 3. Nom du service (fr-header__service)
        Elements service = doc.select(".fr-header__service, [class*=fr-header__service]");
        if (!service.isEmpty()) { passed++; detail.append("service-name "); }

        // 4. Navigation (fr-nav ou nav dans header)
        Elements nav = doc.select(".fr-nav, header nav, .fr-header nav");
        if (!nav.isEmpty()) { passed++; detail.append("nav "); }

        // 5. Indicateur DSFR global (présence du CSS/JS DSFR)
        Elements dsfrAssets = doc.select(
                "link[href*=dsfr], script[src*=dsfr], [class^=fr-], [data-fr-scheme]");
        if (!dsfrAssets.isEmpty()) { passed++; detail.append("dsfr-assets "); }

        score = (double) passed / checks;
        String msg = String.format("En-tête DSFR : %d/%d critères [%s]",
                passed, checks, detail.toString().trim());

        if (passed == checks) {
            return RuleResultDto.success(id(), category(), msg);
        } else if (passed == 0) {
            return RuleResultDto.failure(id(), category(),
                    "Aucun composant DSFR détecté dans l'en-tête");
        }

        return RuleResultDto.partial(id(), category(),
                Math.round(score * 100.0) / 100.0, msg);
    }
}
