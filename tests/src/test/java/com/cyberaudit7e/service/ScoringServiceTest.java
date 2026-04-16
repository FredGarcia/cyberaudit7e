package com.CyberAudit7E.service;

import com.CyberAudit7E.domain.enums.RuleCategory;
import com.CyberAudit7E.dto.RuleResultDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * NIVEAU 1 — Test unitaire pur.
 *
 * Pas d'annotation Spring. Démarrage instantané (< 1 seconde).
 * On teste uniquement la logique métier du scoring.
 *
 * Utilise :
 *   - JUnit Jupiter (@Test, @DisplayName)
 *   - AssertJ (assertThat...) — fluent assertions, bien plus lisibles que JUnit assert
 */
class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService();

    @Test
    @DisplayName("Score de 100% quand toutes les règles passent (toutes catégories)")
    void perfectScore() {
        List<RuleResultDto> allPass = List.of(
                RuleResultDto.success("RGAA-1", RuleCategory.RGAA, "ok"),
                RuleResultDto.success("WCAG-1", RuleCategory.WCAG, "ok"),
                RuleResultDto.success("DSFR-1", RuleCategory.DSFR, "ok")
        );

        Map<String, Double> scores = scoringService.computeScores(allPass);

        assertThat(scores.get("rgaa")).isEqualTo(1.0);
        assertThat(scores.get("wcag")).isEqualTo(1.0);
        assertThat(scores.get("dsfr")).isEqualTo(1.0);
        assertThat(scores.get("global")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Score de 0% quand toutes les règles échouent")
    void zeroScore() {
        List<RuleResultDto> allFail = List.of(
                RuleResultDto.failure("RGAA-1", RuleCategory.RGAA, "ko"),
                RuleResultDto.failure("WCAG-1", RuleCategory.WCAG, "ko"),
                RuleResultDto.failure("DSFR-1", RuleCategory.DSFR, "ko")
        );

        Map<String, Double> scores = scoringService.computeScores(allFail);

        assertThat(scores.get("global")).isZero();
    }

    @Test
    @DisplayName("Formule pondérée RGAA×0.5 + WCAG×0.3 + DSFR×0.2 respectée")
    void weightedFormulaIsRespected() {
        // RGAA moyenne = 0.8, WCAG = 0.6, DSFR = 0.4
        List<RuleResultDto> results = List.of(
                RuleResultDto.partial("RGAA-1", RuleCategory.RGAA, 0.8, "partiel"),
                RuleResultDto.partial("WCAG-1", RuleCategory.WCAG, 0.6, "partiel"),
                RuleResultDto.partial("DSFR-1", RuleCategory.DSFR, 0.4, "partiel")
        );

        Map<String, Double> scores = scoringService.computeScores(results);

        // Attendu : 0.8×0.5 + 0.6×0.3 + 0.4×0.2 = 0.4 + 0.18 + 0.08 = 0.66
        assertThat(scores.get("global")).isEqualTo(0.66, within(0.001));
    }

    @Test
    @DisplayName("Moyenne calculée par catégorie quand plusieurs règles de même catégorie")
    void averagePerCategory() {
        List<RuleResultDto> results = List.of(
                RuleResultDto.partial("RGAA-1", RuleCategory.RGAA, 0.6, ""),
                RuleResultDto.partial("RGAA-2", RuleCategory.RGAA, 1.0, ""),
                RuleResultDto.partial("RGAA-3", RuleCategory.RGAA, 0.8, "")
        );

        Map<String, Double> scores = scoringService.computeScores(results);

        // Moyenne RGAA = (0.6 + 1.0 + 0.8) / 3 = 0.8
        assertThat(scores.get("rgaa")).isEqualTo(0.8);
    }

    @Test
    @DisplayName("Liste vide → tous scores à 0")
    void emptyList() {
        Map<String, Double> scores = scoringService.computeScores(List.of());

        assertThat(scores).containsEntry("rgaa", 0.0)
                          .containsEntry("wcag", 0.0)
                          .containsEntry("dsfr", 0.0)
                          .containsEntry("global", 0.0);
    }
}
