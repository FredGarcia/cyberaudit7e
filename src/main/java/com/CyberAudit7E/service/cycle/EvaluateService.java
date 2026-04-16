package com.CyberAudit7E.service.cycle;

import com.CyberAudit7E.domain.entity.Site;
import com.CyberAudit7E.domain.enums.Phase7E;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Phase 7E : ÉVALUER
 * Collecte les métriques du site et prépare le contexte d'audit.
 * En production : crawl HTTP, extraction DOM, inventaire des composants.
 */
@Service
public class EvaluateService {

    private static final Logger log = LoggerFactory.getLogger(EvaluateService.class);

    /**
     * Évalue le site et le fait transiter vers la phase ÉLABORER.
     *
     * @param site Le site à évaluer
     * @return Le contexte d'évaluation (URL validée et prête)
     */
    public String evaluate(Site site) {
        log.info("[7E-ÉVALUER] Début d'évaluation du site : {} ({})", site.getName(), site.getUrl());

        // POC : validation basique de l'URL
        if (site.getUrl() == null || site.getUrl().isBlank()) {
            throw new IllegalArgumentException("URL du site invalide : " + site.getUrl());
        }

        site.setCurrentPhase(Phase7E.EVALUER);
        log.info("[7E-ÉVALUER] Site {} prêt pour l'audit — URL validée", site.getName());
        return site.getUrl();
    }
}
