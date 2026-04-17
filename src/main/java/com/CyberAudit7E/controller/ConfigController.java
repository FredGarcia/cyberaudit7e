package com.cyberaudit7e.controller;

import com.cyberaudit7e.domain.entity.RuleConfig;
import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.repository.RuleConfigRepository;
import com.cyberaudit7e.service.ScoringService;
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
 * REST Controller pour la configuration des poids de scoring.
 * M4 NOUVEAU : permet de visualiser et ajuster les poids dynamiques.
 *
 * Expose la mécanique interne de la phase ÉQUILIBRER pour le debug
 * et le pilotage manuel du moteur.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final RuleConfigRepository configRepository;
    private final ScoringService scoringService;

    public ConfigController(RuleConfigRepository configRepository,
                            ScoringService scoringService) {
        this.configRepository = configRepository;
        this.scoringService = scoringService;
    }

    /**
     * GET /api/config/weights — Voir les poids actuels.
     */
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
                    "description", config.getDescription() != null ? config.getDescription() : ""
            ));
            if (config.getEnabled()) total += config.getWeight();
        }

        result.put("totalWeight", Math.round(total * 1000.0) / 1000.0);
        result.put("normalized", Math.abs(total - 1.0) < 0.01);

        return result;
    }

    /**
     * PUT /api/config/weights/{category} — Modifier un poids.
     */
    @PutMapping("/weights/{category}")
    @Transactional
    public ResponseEntity<?> updateWeight(
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
                "category", ruleCategory,
                "weight", request.weight(),
                "message", "Poids mis à jour"
        ));
    }

    /**
     * POST /api/config/weights/reset — Remettre les poids par défaut.
     */
    @PostMapping("/weights/reset")
    @Transactional
    public Map<String, Object> resetWeights() {
        for (RuleCategory cat : RuleCategory.values()) {
            scoringService.updateWeight(cat, cat.getDefaultWeight());
        }
        return Map.of(
                "message", "Poids remis aux valeurs par défaut",
                "RGAA", RuleCategory.RGAA.getDefaultWeight(),
                "WCAG", RuleCategory.WCAG.getDefaultWeight(),
                "DSFR", RuleCategory.DSFR.getDefaultWeight()
        );
    }

    // ── Request DTO ──

    public record WeightUpdateRequest(
            @NotNull(message = "Le poids est obligatoire")
            @DecimalMin(value = "0.0", message = "Le poids doit être >= 0")
            @DecimalMax(value = "1.0", message = "Le poids doit être <= 1")
            Double weight
    ) {}
}
