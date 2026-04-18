package com.cyberaudit7e.controller;

import com.cyberaudit7e.domain.entity.RuleConfig;
import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.repository.RuleConfigRepository;
import com.cyberaudit7e.service.ScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration des poids de scoring.
 * M6 : documentation OpenAPI complète.
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "Configuration", description = "Gestion dynamique des poids de scoring par catégorie (RGAA, WCAG, DSFR)")
public class ConfigController {

    private final RuleConfigRepository configRepository;
    private final ScoringService scoringService;

    public ConfigController(RuleConfigRepository configRepository,
                            ScoringService scoringService) {
        this.configRepository = configRepository;
        this.scoringService = scoringService;
    }

    @Operation(
            summary = "Voir les poids de scoring",
            description = "Retourne les poids actuels par catégorie (RGAA, WCAG, DSFR) " +
                    "avec indication de normalisation (totalWeight ≈ 1.0). " +
                    "Ces poids sont modifiés automatiquement par la boucle de rétroaction (phase ÉQUILIBRER) " +
                    "ou manuellement via PUT."
    )
    @GetMapping("/weights")
    @Transactional(readOnly = true)
    public Map<String, Object> getWeights() {
        List<RuleConfig> configs = configRepository.findAll();
        Map<String, Object> result = new LinkedHashMap<>();
        double total = 0.0;
        for (RuleConfig config : configs) {
            result.put(config.getCategory().name(), Map.of(
                    "weight", config.getWeight(),
                    "enabled", config.getEnabled(),
                    "description", config.getDescription() != null ? config.getDescription() : ""));
            if (config.getEnabled()) total += config.getWeight();
        }
        result.put("totalWeight", Math.round(total * 1000.0) / 1000.0);
        result.put("normalized", Math.abs(total - 1.0) < 0.01);
        return result;
    }

    @Operation(
            summary = "Modifier le poids d'une catégorie",
            description = "Met à jour le poids de scoring pour la catégorie spécifiée. " +
                    "Valeur entre 0.0 et 1.0. Penser à normaliser après modification."
    )
    @PutMapping("/weights/{category}")
    @Transactional
    public ResponseEntity<?> updateWeight(
            @Parameter(description = "Catégorie (RGAA, WCAG, DSFR)", example = "RGAA")
            @PathVariable String category,
            @Valid @RequestBody WeightUpdateRequest request) {
        RuleCategory ruleCategory;
        try {
            ruleCategory = RuleCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Catégorie invalide : " + category,
                                 "valid", List.of("RGAA", "WCAG", "DSFR")));
        }
        scoringService.updateWeight(ruleCategory, request.weight());
        return ResponseEntity.ok(Map.of(
                "category", ruleCategory, "weight", request.weight(), "message", "Poids mis à jour"));
    }

    @Operation(
            summary = "Remettre les poids par défaut",
            description = "Réinitialise : RGAA=0.50, WCAG=0.30, DSFR=0.20 (somme = 1.0)."
    )
    @PostMapping("/weights/reset")
    @Transactional
    public Map<String, Object> resetWeights() {
        for (RuleCategory cat : RuleCategory.values()) {
            scoringService.updateWeight(cat, cat.getDefaultWeight());
        }
        return Map.of("message", "Poids remis aux valeurs par défaut",
                "RGAA", RuleCategory.RGAA.getDefaultWeight(),
                "WCAG", RuleCategory.WCAG.getDefaultWeight(),
                "DSFR", RuleCategory.DSFR.getDefaultWeight());
    }

    public record WeightUpdateRequest(
            @NotNull(message = "Le poids est obligatoire")
            @DecimalMin(value = "0.0", message = "Le poids doit être >= 0")
            @DecimalMax(value = "1.0", message = "Le poids doit être <= 1")
            Double weight
    ) {}
}
