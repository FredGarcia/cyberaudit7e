package com.cyberaudit7e.controller;

import com.cyberaudit7e.domain.entity.Site;
import com.cyberaudit7e.dto.PagedResponse;
import com.cyberaudit7e.dto.SiteDto;
import com.cyberaudit7e.repository.SiteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller pour la gestion des sites.
 * M6 : documentation OpenAPI complète + pagination.
 */
@RestController
@RequestMapping("/api/sites")
@Tag(name = "Sites", description = "Gestion des sites à auditer — CRUD + recherche + pagination")
public class SiteController {

    private final SiteRepository siteRepository;

    public SiteController(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Operation(
            summary = "Enregistrer un nouveau site",
            description = "Ajoute un site au registre d'audit. L'URL doit être unique. " +
                    "Le site est initialisé à la phase ÉVALUER du cycle 7E."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Site créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "URL déjà enregistrée")
    })
    @PostMapping
    public ResponseEntity<?> createSite(@Valid @RequestBody CreateSiteRequest request) {
        if (siteRepository.existsByUrl(request.url())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Un site avec cette URL existe déjà",
                                 "url", request.url()));
        }
        Site site = siteRepository.save(new Site(request.url(), request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(SiteDto.from(site));
    }

    @Operation(
            summary = "Lister tous les sites (paginé)",
            description = "Retourne la liste paginée des sites enregistrés, triés par date de création."
    )
    @GetMapping
    @Transactional(readOnly = true)
    public PagedResponse<SiteDto> listSites(
            @Parameter(description = "Numéro de page (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        return PagedResponse.from(
                siteRepository.findAll(pageable).map(SiteDto::from));
    }

    @Operation(summary = "Détail d'un site")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Site trouvé"),
            @ApiResponse(responseCode = "404", description = "Site introuvable")
    })
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<SiteDto> getSite(
            @Parameter(description = "ID du site", example = "1")
            @PathVariable Long id) {
        return siteRepository.findById(id)
                .map(SiteDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Rechercher des sites par nom",
            description = "Recherche insensible à la casse dans les noms de sites."
    )
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public List<SiteDto> searchSites(
            @Parameter(description = "Terme de recherche", example = "gouv")
            @RequestParam String name) {
        return siteRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(SiteDto::from)
                .toList();
    }

    @Operation(summary = "Supprimer un site et ses rapports")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Site supprimé"),
            @ApiResponse(responseCode = "404", description = "Site introuvable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(
            @Parameter(description = "ID du site")
            @PathVariable Long id) {
        if (!siteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        siteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateSiteRequest(
            @NotBlank(message = "L'URL est obligatoire") String url,
            @NotBlank(message = "Le nom est obligatoire") String name
    ) {}
}
