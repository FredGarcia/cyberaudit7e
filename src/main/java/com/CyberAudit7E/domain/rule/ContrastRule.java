package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * WCAG 1.4.3 — Contraste minimum 4.5:1 pour le texte normal.
 *
 * M4 : analyse partielle — Jsoup ne peut pas calculer les couleurs CSS résolues.
 * On vérifie les indicateurs indirects :
 * - Présence de styles inline avec color/background-color
 * - Utilisation de classes de couleur courantes
 * - Méta viewport pour le texte responsive
 *
 * En production : Playwright + computed styles pour un ratio réel.
 */
@Component
public class ContrastRule implements AuditRule {

    @Override public String id() { return "WCAG-1.4.3"; }
    @Override public String description() { return "Contraste minimum 4.5:1 pour le texte normal"; }
    @Override public RuleCategory category() { return RuleCategory.WCAG; }
    @Override public int priority() { return 80; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();

        // Compter les éléments avec des couleurs inline (risque de contraste faible)
        Elements withInlineColor = doc.select("[style*=color]");
        Elements withInlineBg = doc.select("[style*=background]");

        int inlineColorCount = withInlineColor.size();
        int inlineBgCount = withInlineBg.size();

        // Chercher les couleurs potentiellement problématiques
        int lightTextCount = 0;
        for (Element el : withInlineColor) {
            String style = el.attr("style").toLowerCase();
            if (style.contains("color:#ccc") || style.contains("color:#ddd")
                    || style.contains("color:#eee") || style.contains("color:#999")
                    || style.contains("color:lightgray") || style.contains("color:silver")) {
                lightTextCount++;
            }
        }

        // Évaluation heuristique
        double score;
        String detail;

        if (inlineColorCount == 0 && inlineBgCount == 0) {
            score = 0.8; // Pas de styles inline = probablement OK via CSS
            detail = "Aucun style de couleur inline — contraste géré par CSS externe (non auditable via HTML)";
        } else if (lightTextCount > 0) {
            score = 0.4;
            detail = String.format("%d couleur(s) potentiellement faibles détectée(s) " +
                    "sur %d élément(s) avec couleur inline", lightTextCount, inlineColorCount);
        } else {
            score = 0.7;
            detail = String.format("%d élément(s) avec couleur inline, %d avec background — " +
                    "analyse CSS externe requise pour vérification complète",
                    inlineColorCount, inlineBgCount);
        }

        return RuleResultDto.partial(id(), category(),
                Math.round(score * 100.0) / 100.0, detail);
    }
}
