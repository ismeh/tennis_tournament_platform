package com.tfm.tennis_platform.application.services.strategies.draw;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolationDrawGeneratorTest {

    private final ConsolationDrawGenerator generator = new ConsolationDrawGenerator();

    @Test
    void generatesSingleConsolationDraw() {
        Stage stage = Stage.builder()
                .stageNumber(2)
                .stageType(StageType.CONSOLATION)
                .build();

        List<Inscription> inscriptions = List.of(
                inscription(), inscription(), inscription(), inscription()
        );

        List<Draw> draws = generator.generateDraws(stage, inscriptions);

        assertThat(draws).hasSize(1);
        assertThat(draws.get(0).getDrawType()).isEqualTo(DrawType.CONSOLATION);
        assertThat(draws.get(0).getStageId()).isEqualTo(stage.getId());
    }

    @Test
    void drawNameIncludesPlayerCount() {
        Stage stage = Stage.builder()
                .stageNumber(2)
                .stageType(StageType.CONSOLATION)
                .build();

        List<Inscription> inscriptions = List.of(
                inscription(), inscription(), inscription(), inscription(),
                inscription(), inscription()
        );

        List<Draw> draws = generator.generateDraws(stage, inscriptions);

        assertThat(draws.get(0).getDrawName()).contains("6 Players");
    }

    @Test
    void drawHasConsolationType() {
        Stage stage = Stage.builder()
                .stageNumber(1)
                .stageType(StageType.CONSOLATION)
                .build();

        List<Draw> draws = generator.generateDraws(stage, List.of(inscription(), inscription()));

        assertThat(draws).allMatch(d -> d.getDrawType() == DrawType.CONSOLATION);
    }

    @Test
    void drawHasStageId() {
        UUID stageId = UUID.randomUUID();
        Stage stage = Stage.builder()
                .id(stageId)
                .stageNumber(1)
                .stageType(StageType.CONSOLATION)
                .build();

        List<Draw> draws = generator.generateDraws(stage, List.of(inscription()));

        assertThat(draws.get(0).getStageId()).isEqualTo(stageId);
    }

    private Inscription inscription() {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .build();
    }
}
