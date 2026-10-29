package com.tfm.tennis_platform.application.services.strategies.draw;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DoubleEliminationDrawGenerator")
class DoubleEliminationDrawGeneratorTest {

    private DoubleEliminationDrawGenerator generator;
    private Stage stage;

    @BeforeEach
    void setUp() {
        generator = new DoubleEliminationDrawGenerator();
        stage = Stage.builder()
                .id(UUID.randomUUID())
                .stageNumber(1)
                .stageType(StageType.DOUBLE_ELIMINATION)
                .build();
    }

    private List<Inscription> createInscriptions(int count) {
        List<Inscription> inscriptions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            inscriptions.add(Inscription.builder()
                    .id(UUID.randomUUID())
                    .build());
        }
        return inscriptions;
    }

    @Test
    @DisplayName("Should always create exactly 2 draws: winners and losers brackets")
    void shouldCreateTwoDraws() {
        List<Draw> draws = generator.generateDraws(stage, createInscriptions(8));

        assertThat(draws).hasSize(2);
    }

    @Test
    @DisplayName("First draw should be winners bracket with ELIMINATION type")
    void shouldCreateWinnersBracket() {
        List<Draw> draws = generator.generateDraws(stage, createInscriptions(8));

        Draw winners = draws.get(0);
        assertThat(winners.getDrawType()).isEqualTo(DrawType.ELIMINATION);
        assertThat(winners.getLabel()).isEqualTo("Ganadores");
        assertThat(winners.getDrawName()).contains("Winners Bracket");
        assertThat(winners.getDrawName()).contains("8 Players");
    }

    @Test
    @DisplayName("Second draw should be losers bracket with DOUBLE_ELIMINATION type")
    void shouldCreateLosersBracket() {
        List<Draw> draws = generator.generateDraws(stage, createInscriptions(8));

        Draw losers = draws.get(1);
        assertThat(losers.getDrawType()).isEqualTo(DrawType.DOUBLE_ELIMINATION);
        assertThat(losers.getLabel()).isEqualTo("Perdedores");
        assertThat(losers.getDrawName()).isEqualTo("Losers Bracket");
    }

    @Test
    @DisplayName("Both draws should reference the same stage")
    void bothDrawsShouldReferenceSameStage() {
        List<Draw> draws = generator.generateDraws(stage, createInscriptions(4));

        assertThat(draws).allMatch(draw -> draw.getStageId().equals(stage.getId()));
    }

    @Test
    @DisplayName("Should work with any number of players")
    void shouldWorkWithAnyPlayerCount() {
        for (int count : List.of(2, 3, 5, 8, 16)) {
            List<Draw> draws = generator.generateDraws(stage, createInscriptions(count));
            assertThat(draws).hasSize(2);
            assertThat(draws.get(0).getDrawName()).contains(count + " Players");
        }
    }
}
