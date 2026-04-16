package com.CyberAudit7E.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NIVEAU 1 — Test unitaire de l'enum Phase7E.
 *
 * Démontre les tests paramétrés JUnit (@ParameterizedTest + @CsvSource)
 * pour éviter la duplication.
 */
class Phase7ETest {

    @ParameterizedTest
    @CsvSource({
            "EVALUER,    ELABORER",
            "ELABORER,   EXECUTER",
            "EXECUTER,   EXAMINER",
            "EXAMINER,   EVOLUER",
            "EVOLUER,    EMETTRE",
            "EMETTRE,    EQUILIBRER"
    })
    @DisplayName("Chaque phase avance à la phase suivante")
    void nextPhaseTransitions(Phase7E current, Phase7E expectedNext) {
        assertThat(current.next()).isEqualTo(expectedNext);
    }

    @Test
    @DisplayName("Après EQUILIBRER, le cycle reprend à EVALUER (boucle cybernétique)")
    void cycleLoopsBack() {
        assertThat(Phase7E.EQUILIBRER.next()).isEqualTo(Phase7E.EVALUER);
    }

    @Test
    @DisplayName("7 phases au total dans le cycle")
    void sevenPhasesInCycle() {
        assertThat(Phase7E.values()).hasSize(7);
    }

    @Test
    @DisplayName("Chaque phase a un label et une description non-vides")
    void allPhasesHaveMetadata() {
        for (Phase7E phase : Phase7E.values()) {
            assertThat(phase.getLabel()).isNotBlank();
            assertThat(phase.getDescription()).isNotBlank();
        }
    }
}
