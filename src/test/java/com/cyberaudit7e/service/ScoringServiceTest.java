package com.cyberaudit7e.service;

import com.cyberaudit7e.domain.entity.RuleConfig;
import com.cyberaudit7e.domain.enums.RuleCategory;
import com.cyberaudit7e.dto.RuleResultDto;
import com.cyberaudit7e.repository.RuleConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService M4 — Poids dynamiques")
class ScoringServiceTest {

    @Mock
    private RuleConfigRepository ruleConfigRepository;

    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService(ruleConfigRepository);
    }

    @Nested
    @DisplayName("loadWeights()")
    class LoadWeights {

        @Test
        @DisplayName("Charge les poids depuis la BDD")
        void shouldLoadWeightsFromDb() {
            when(ruleConfigRepository.findByEnabledTrue()).thenReturn(List.of(
                    new RuleConfig(RuleCategory.RGAA, 0.5, "RGAA"),
                    new RuleConfig(RuleCategory.WCAG, 0.3, "WCAG"),
                    new RuleConfig(RuleCategory.DSFR, 0.2, "DSFR")
            ));

            Map<RuleCategory, Double> weights = scoringService.loadWeights();

            assertEquals(0.5, weights.get(RuleCategory.RGAA));
            assertEquals(0.3, weights.get(RuleCategory.WCAG));
            assertEquals(0.2, weights.get(RuleCategory.DSFR));
        }

        @Test
        @DisplayName("Fallback sur les valeurs par défaut si BDD vide")
        void shouldFallbackToDefaults() {
            when(ruleConfigRepository.findByEnabledTrue()).thenReturn(List.of());

            Map<RuleCategory, Double> weights = scoringService.loadWeights();

            assertEquals(RuleCategory.RGAA.getDefaultWeight(), weights.get(RuleCategory.RGAA));
            assertEquals(RuleCategory.WCAG.getDefaultWeight(), weights.get(RuleCategory.WCAG));
            assertEquals(RuleCategory.DSFR.getDefaultWeight(), weights.get(RuleCategory.DSFR));
        }
    }

    @Nested
    @DisplayName("computeScores()")
    class ComputeScores {

        @BeforeEach
        void setUpWeights() {
            // Retourner les poids par défaut pour tous les tests de scoring
            when(ruleConfigRepository.findByEnabledTrue()).thenReturn(List.of(
                    new RuleConfig(RuleCategory.RGAA, 0.5, "RGAA"),
                    new RuleConfig(RuleCategory.WCAG, 0.3, "WCAG"),
                    new RuleConfig(RuleCategory.DSFR, 0.2, "DSFR")
            ));
        }

        @Test
        @DisplayName("Score global = RGAA×0.5 + WCAG×0.3 + DSFR×0.2")
        void shouldComputeWeightedScore() {
            List<RuleResultDto> results = List.of(
                    RuleResultDto.success("RGAA-1", RuleCategory.RGAA, "OK"),    // 1.0
                    RuleResultDto.failure("RGAA-2", RuleCategory.RGAA, "KO"),    // 0.0
                    RuleResultDto.success("WCAG-1", RuleCategory.WCAG, "OK"),    // 1.0
                    RuleResultDto.partial("DSFR-1", RuleCategory.DSFR, 0.6, "Partiel") // 0.6
            );

            Map<String, Double> scores = scoringService.computeScores(results);

            // RGAA: (1.0 + 0.0) / 2 = 0.5
            assertEquals(0.5, scores.get("rgaa"));
            // WCAG: 1.0 / 1 = 1.0
            assertEquals(1.0, scores.get("wcag"));
            // DSFR: 0.6 / 1 = 0.6
            assertEquals(0.6, scores.get("dsfr"));
            // Global: 0.5×0.5 + 1.0×0.3 + 0.6×0.2 = 0.25 + 0.30 + 0.12 = 0.67
            assertEquals(0.67, scores.get("global"));
        }

        @Test
        @DisplayName("Score parfait = 1.0")
        void shouldReturnPerfectScore() {
            List<RuleResultDto> results = List.of(
                    RuleResultDto.success("RGAA-1", RuleCategory.RGAA, "OK"),
                    RuleResultDto.success("WCAG-1", RuleCategory.WCAG, "OK"),
                    RuleResultDto.success("DSFR-1", RuleCategory.DSFR, "OK")
            );

            Map<String, Double> scores = scoringService.computeScores(results);
            assertEquals(1.0, scores.get("global"));
        }

        @Test
        @DisplayName("Score zéro si tout échoue")
        void shouldReturnZeroScore() {
            List<RuleResultDto> results = List.of(
                    RuleResultDto.failure("RGAA-1", RuleCategory.RGAA, "KO"),
                    RuleResultDto.failure("WCAG-1", RuleCategory.WCAG, "KO"),
                    RuleResultDto.failure("DSFR-1", RuleCategory.DSFR, "KO")
            );

            Map<String, Double> scores = scoringService.computeScores(results);
            assertEquals(0.0, scores.get("global"));
        }

        @Test
        @DisplayName("Inclut les poids utilisés dans la réponse")
        void shouldIncludeWeightsInResponse() {
            List<RuleResultDto> results = List.of(
                    RuleResultDto.success("RGAA-1", RuleCategory.RGAA, "OK")
            );

            Map<String, Double> scores = scoringService.computeScores(results);

            assertEquals(0.5, scores.get("weight_rgaa"));
            assertEquals(0.3, scores.get("weight_wcag"));
            assertEquals(0.2, scores.get("weight_dsfr"));
        }
    }

    @Nested
    @DisplayName("updateWeight()")
    class UpdateWeight {

        @Test
        @DisplayName("Met à jour le poids en BDD")
        void shouldUpdateWeightInDb() {
            RuleConfig config = new RuleConfig(RuleCategory.RGAA, 0.5, "RGAA");
            when(ruleConfigRepository.findByCategory(RuleCategory.RGAA))
                    .thenReturn(Optional.of(config));

            scoringService.updateWeight(RuleCategory.RGAA, 0.6);

            assertEquals(0.6, config.getWeight());
            verify(ruleConfigRepository).save(config);
        }
    }
}
