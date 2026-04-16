package com.CyberAudit7E.controller;

import com.CyberAudit7E.domain.entity.Site;
import com.CyberAudit7E.dto.SiteDto;
import com.CyberAudit7E.repository.SiteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller pour la gestion des sites.
 *
 * M3 : retourne des SiteDto (pas des entités JPA) pour éviter :
 * - Sérialisation circulaire Site ↔ AuditReport
 * - LazyInitializationException hors transaction
 * - Exposition de détails internes (colonnes, relations)
 *
 * Règle d'or JPA : Entités = couche interne, DTOs = couche API.
 */
@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteRepository siteRepository;

    public SiteController(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    /**
     * POST /api/sites — Enregistrer un nouveau site.
     */
    @PostMapping
    public ResponseEntity<?> createSite(@Valid @RequestBody CreateSiteRequest request) {
        if (siteRepository.existsByUrl(request.url())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Un site avec cette URL existe déjà",
                                 "url", request.url()));
        }

        Site site = new Site(request.url(), request.name());
        site = siteRepository.save(site);
        return ResponseEntity.status(HttpStatus.CREATED).body(SiteDto.from(site));
    }

    /**
     * GET /api/sites — Lister tous les sites.
     * @Transactional(readOnly) : optimise les lectures JPA,
     * et garde la session ouverte pour le lazy loading dans SiteDto.from().
     */
    @GetMapping
    @Transactional(readOnly = true)
    public List<SiteDto> listSites() {
        return siteRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SiteDto::from)
                .toList();
    }

    /**
     * GET /api/sites/{id} — Détail d'un site.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<SiteDto> getSite(@PathVariable Long id) {
        return siteRepository.findById(id)
                .map(SiteDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/sites/search?name=xxx — Recherche par nom.
     */
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public List<SiteDto> searchSites(@RequestParam String name) {
        return siteRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(SiteDto::from)
                .toList();
    }

    /**
     * DELETE /api/sites/{id} — Supprimer un site et ses rapports (CASCADE).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        if (!siteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        siteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Request DTO ──

    public record CreateSiteRequest(
            @NotBlank(message = "L'URL est obligatoire")
            String url,
            @NotBlank(message = "Le nom est obligatoire")
            String name
    ) {}
}
