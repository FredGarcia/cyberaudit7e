package com.cyberaudit7e.controller;

import com.cyberaudit7e.domain.entity.Site;
import com.cyberaudit7e.repository.SiteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller pour la gestion des sites à auditer.
 * Inspiré du registre d'organes GitManager.
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
        return ResponseEntity.status(HttpStatus.CREATED).body(site);
    }

    /**
     * GET /api/sites — Lister tous les sites.
     */
    @GetMapping
    public List<Site> listSites() {
        return siteRepository.findAll();
    }

    /**
     * GET /api/sites/{id} — Détail d'un site.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Site> getSite(@PathVariable Long id) {
        return siteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/sites/{id} — Supprimer un site.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        if (siteRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        siteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Request DTO interne ──

    public record CreateSiteRequest(
            @NotBlank(message = "L'URL est obligatoire")
            String url,
            @NotBlank(message = "Le nom est obligatoire")
            String name
    ) {}
}
