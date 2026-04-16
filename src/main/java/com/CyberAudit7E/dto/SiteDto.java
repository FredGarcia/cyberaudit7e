package com.CyberAudit7E.dto;

import com.CyberAudit7E.domain.entity.Site;

import java.time.LocalDateTime;

/**
 * DTO de réponse pour un Site.
 *
 * M3 : nécessaire pour éviter la sérialisation circulaire
 * Site → reports → site → reports causée par @OneToMany/@ManyToOne.
 *
 * Règle d'or JPA : ne jamais exposer les entités directement dans les
 * réponses REST. Toujours passer par un DTO.
 *
 * @param id           Identifiant du site
 * @param url          URL du site
 * @param name         Nom lisible
 * @param currentPhase Phase 7E courante
 * @param auditsCount  Nombre d'audits réalisés
 * @param lastScore    Score du dernier audit (null si aucun)
 * @param createdAt    Date de création
 * @param updatedAt    Date de dernière modification
 */
public record SiteDto(
        Long id,
        String url,
        String name,
        String currentPhase,
        int auditsCount,
        Double lastScore,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * Factory depuis une entité Site.
     */
    public static SiteDto from(Site site) {
        Double lastScore = null;
        if (site.getReports() != null && !site.getReports().isEmpty()) {
            lastScore = site.getReports().getFirst().getScoreGlobal();
        }
        return new SiteDto(
                site.getId(),
                site.getUrl(),
                site.getName(),
                site.getCurrentPhase().name(),
                site.getReports() != null ? site.getReports().size() : 0,
                lastScore,
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }
}
