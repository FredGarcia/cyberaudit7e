package com.cyberaudit7e.controller;

import com.cyberaudit7e.domain.entity.RuleConfig;
import com.cyberaudit7e.domain.entity.SystemSetting;
import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.integration.TicketOrchestrator;
import com.cyberaudit7e.repository.RuleConfigRepository;
import com.cyberaudit7e.repository.SystemSettingRepository;
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
 * Configuration des poids de scoring et des parametres systeme.
 * MISE A JOUR : ajoute les endpoints /api/config/settings pour le seuil de
 * ticket.
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "Configuration", description = "Poids de scoring + parametres systeme (seuil de ticket)")
public class ConfigController {

    private final RuleConfigRepository configRepository;
    private final ScoringService scoringService;
    private final SystemSettingRepository settingRepository;
    private final TicketOrchestrator ticketOrchestrator;

    public ConfigController(RuleConfigRepository configRepository,
            ScoringService scoringService,
            SystemSettingRepository settingRepository,
            TicketOrchestrator ticketOrchestrator) {
        this.configRepository = configRepository;
        this.scoringService = scoringService;
        this.settingRepository = settingRepository;
        this.ticketOrchestrator = ticketOrchestrator;
    }

    // ═══════════════════════════════════════════
    // POIDS DE SCORING (existant)
    // ═══════════════════════════════════════════

    @Operation(summary = "Voir les poids de scoring")
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
            if (config.getEnabled())
                total += config.getWeight();
        }
        result.put("totalWeight", Math.round(total * 1000.0) / 1000.0);
        result.put("normalized", Math.abs(total - 1.0) < 0.01);
        return result;
    }

    @Operation(summary = "Modifier le poids d'une categorie")
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
                    .body(Map.of("error", "Categorie invalide : " + category,
                            "valid", List.of("RGAA", "WCAG", "DSFR")));
        }
        scoringService.updateWeight(ruleCategory, request.weight());
        return ResponseEntity
                .ok(Map.of("category", ruleCategory, "weight", request.weight(), "message", "Poids mis a jour"));
    }

    @Operation(summary = "Remettre les poids par defaut")
    @PostMapping("/weights/reset")
    @Transactional
    public Map<String, Object> resetWeights() {
        for (RuleCategory cat : RuleCategory.values()) {
            scoringService.updateWeight(cat, cat.getDefaultWeight());
        }
        return Map.of("message", "Poids remis aux valeurs par defaut",
                "RGAA", RuleCategory.RGAA.getDefaultWeight(),
                "WCAG", RuleCategory.WCAG.getDefaultWeight(),
                "DSFR", RuleCategory.DSFR.getDefaultWeight());
    }

    // ═══════════════════════════════════════════
    // PARAMETRES SYSTEME (NOUVEAU)
    // ═══════════════════════════════════════════

    @Operation(summary = "Voir tous les parametres systeme", description = "Retourne tous les parametres configurables, dont le seuil de creation auto de tickets.")
    @GetMapping("/settings")
    @Transactional(readOnly = true)
    public Map<String, Object> getAllSettings() {
        Map<String, Object> result = new LinkedHashMap<>();
        settingRepository.findAll().forEach(s -> result.put(s.getKey(), Map.of(
                "value", s.getValue(),
                "description", s.getDescription() != null ? s.getDescription() : "",
                "updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : "")));
        return result;
    }

    @Operation(summary = "Voir le seuil de creation auto de tickets", description = "Retourne le seuil en dessous duquel un audit cree automatiquement un ticket de securite. "
            +
            "Valeur entre 0.0 (desactive) et 1.0 (toujours creer). Par defaut : 0.50 (50%).")
    @GetMapping("/settings/ticket-threshold")
    public Map<String, Object> getTicketThreshold() {
        double threshold = ticketOrchestrator.getAutoTicketThreshold();
        return Map.of(
                "threshold", threshold,
                "thresholdPercent", Math.round(threshold * 100),
                "description", "Score en dessous duquel un ticket est cree automatiquement",
                "info", String.format("Un audit avec un score < %.0f%% creera un ticket", threshold * 100));
    }

    @Operation(summary = "Modifier le seuil de creation auto de tickets", description = "Modifie le seuil. Valeur entre 0.0 (jamais creer de ticket auto) et 1.0 (toujours creer). "
            +
            "Exemples : 0.3 = ticket si score < 30%, 0.5 = ticket si score < 50%, 0.7 = ticket si score < 70%.")
    @PutMapping("/settings/ticket-threshold")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateTicketThreshold(
            @Valid @RequestBody ThresholdUpdateRequest request) {

        double value = request.threshold();
        String key = "ticket.auto.threshold";

        SystemSetting setting = settingRepository.findById(key)
                .orElse(new SystemSetting(key, String.valueOf(value),
                        "Seuil de score en dessous duquel un ticket est cree automatiquement"));

        setting.setValue(String.valueOf(value));
        settingRepository.save(setting);

        return ResponseEntity.ok(Map.of(
                "threshold", value,
                "thresholdPercent", Math.round(value * 100),
                "message", String.format("Seuil mis a jour : un ticket sera cree si le score est inferieur a %.0f%%",
                        value * 100)));
    }

    // ═══════════════════════════════════════════
    // MODIFIER UN PARAMETRE GENERIQUE
    // ═══════════════════════════════════════════

    @Operation(summary = "Modifier un parametre systeme generique")
    @PutMapping("/settings/{key}")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateSetting(
            @Parameter(description = "Cle du parametre") @PathVariable String key,
            @RequestBody Map<String, String> body) {

        String value = body.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le champ 'value' est requis"));
        }

        SystemSetting setting = settingRepository.findById(key)
                .orElse(new SystemSetting(key, value, body.getOrDefault("description", "")));

        setting.setValue(value);
        if (body.containsKey("description")) {
            setting.setDescription(body.get("description"));
        }
        settingRepository.save(setting);

        return ResponseEntity.ok(Map.of(
                "key", key,
                "value", value,
                "message", "Parametre mis a jour"));
    }

    // ═══════════════════════════════════════════
    // DTOs
    // ═══════════════════════════════════════════

    public record WeightUpdateRequest(
            @NotNull(message = "Le poids est obligatoire") @DecimalMin(value = "0.0") @DecimalMax(value = "1.0") Double weight) {
    }

    public record ThresholdUpdateRequest(
            @NotNull(message = "Le seuil est obligatoire") @DecimalMin(value = "0.0", message = "Le seuil doit etre >= 0") @DecimalMax(value = "1.0", message = "Le seuil doit etre <= 1") Double threshold) {
    }
}