package com.cyberaudit7e.domain.rule;

import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * WCAG 1.4.4 — Le texte peut-il être redimensionné jusqu'à 200% ?
 * M4 NOUVEAU : vérifie que la meta viewport n'empêche pas le zoom.
 * user-scalable=no ou maximum-scale=1 bloquent l'agrandissement.
 */
@Component
public class MetaViewportRule implements AuditRule {

    @Override public String id() { return "WCAG-1.4.4"; }
    @Override public String description() { return "Viewport n'empêche pas le zoom utilisateur"; }
    @Override public RuleCategory category() { return RuleCategory.WCAG; }
    @Override public int priority() { return 15; }

    @Override
    public RuleResultDto evaluate(AuditContext context) {
        if (!context.hasDocument()) {
            return RuleResultDto.failure(id(), category(), "Impossible de crawler la page");
        }

        Document doc = context.document().get();
        Element viewport = doc.selectFirst("meta[name=viewport]");

        if (viewport == null) {
            return RuleResultDto.partial(id(), category(), 0.6,
                    "Meta viewport absente — responsive non garanti");
        }

        String content = viewport.attr("content").toLowerCase().replaceAll("\\s", "");

        boolean blocksZoom = false;
        StringBuilder issues = new StringBuilder();

        // Vérifier user-scalable=no
        if (content.contains("user-scalable=no") || content.contains("user-scalable=0")) {
            blocksZoom = true;
            issues.append("user-scalable=no ");
        }

        // Vérifier maximum-scale <= 1
        if (content.contains("maximum-scale=")) {
            try {
                String maxScale = content.split("maximum-scale=")[1].split(",")[0];
                double scale = Double.parseDouble(maxScale);
                if (scale <= 1.0) {
                    blocksZoom = true;
                    issues.append(String.format("maximum-scale=%.1f ", scale));
                }
            } catch (Exception ignored) {}
        }

        if (blocksZoom) {
            return RuleResultDto.failure(id(), category(),
                    "Zoom bloqué par viewport : " + issues.toString().trim());
        }

        return RuleResultDto.success(id(), category(),
                "Viewport OK — zoom utilisateur autorisé");
    }
}
