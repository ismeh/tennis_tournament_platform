package com.tfm.tennis_platform.application.services.strategies.draw;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoundRobinDrawGenerator")
class RoundRobinDrawGeneratorTest {

    private RoundRobinDrawGenerator generator;
    private Stage stage;

    @BeforeEach
    void setUp() {
        generator = new RoundRobinDrawGenerator();
        stage = Stage.builder()
                .id(UUID.randomUUID())
                .stageNumber(1)
                .stageType(StageType.ROUND_ROBIN)
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

    @Nested
    @DisplayName("Group generation")
    class GroupTests {

        @Test
        @DisplayName("Should create single group when 6 or fewer players")
        void shouldCreateSingleGroupForSmallCount() {
            List<Draw> draws = generator.generateDraws(stage, createInscriptions(4));

            assertThat(draws).hasSize(1);
            assertThat(draws.get(0).getDrawType()).isEqualTo(DrawType.ROUND_ROBIN);
            assertThat(draws.get(0).getDrawName()).isEqualTo("Grupo unico");
            assertThat(draws.get(0).getGroupIndex()).isNull();
        }

        @Test
        @DisplayName("Should create single group for exactly 6 players")
        void shouldCreateSingleGroupFor6Players() {
            List<Draw> draws = generator.generateDraws(stage, createInscriptions(6));

            assertThat(draws).hasSize(1);
            assertThat(draws.get(0).getDrawName()).isEqualTo("Grupo unico");
        }

        @Test
        @DisplayName("Should create multiple groups when more than 6 players")
        void shouldCreateMultipleGroupsFor7Players() {
            List<Draw> draws = generator.generateDraws(stage, createInscriptions(7));

            assertThat(draws).hasSize(2);
            assertThat(draws.get(0).getDrawName()).isEqualTo("Group A");
            assertThat(draws.get(0).getGroupIndex()).isEqualTo(0);
            assertThat(draws.get(1).getDrawName()).isEqualTo("Group B");
            assertThat(draws.get(1).getGroupIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should create 3 groups for 12 players")
        void shouldCreate3GroupsFor12Players() {
            List<Draw> draws = generator.generateDraws(stage, createInscriptions(12));

            assertThat(draws).hasSize(3);
            assertThat(draws.get(0).getDrawName()).isEqualTo("Group A");
            assertThat(draws.get(1).getDrawName()).isEqualTo("Group B");
            assertThat(draws.get(2).getDrawName()).isEqualTo("Group C");
        }

        @Test
        @DisplayName("All groups should reference the same stage")
        void allGroupsShouldReferenceSameStage() {
            List<Draw> draws = generator.generateDraws(stage, createInscriptions(8));

            assertThat(draws).allMatch(draw -> draw.getStageId().equals(stage.getId()));
        }

        @Test
        @DisplayName("All groups should have ROUND_ROBIN draw type")
        void allGroupsShouldHaveRoundRobinType() {
            List<Draw> draws = generator.generateDraws(stage, createInscriptions(10));

            assertThat(draws).allMatch(draw -> draw.getDrawType() == DrawType.ROUND_ROBIN);
        }
    }
}
