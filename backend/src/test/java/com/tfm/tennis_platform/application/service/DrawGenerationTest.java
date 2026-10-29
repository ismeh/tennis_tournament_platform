package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.services.DrawGenerationService;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DrawGenerationTest {

    @Autowired
    private DrawGenerationService drawGenerationService;

    @Test
    void shouldGenerateSingleEliminationDrawForMainStage() {
        Stage stage = Stage.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .stageNumber(1)
                .stageType(StageType.MAIN)
                .build();

        List<Inscription> inscriptions = List.of(
                createInscription(),
                createInscription(),
                createInscription(),
                createInscription()
        );

        var draws = drawGenerationService.generateDrawsForStage(stage, inscriptions);

        assertEquals(1, draws.size());
        assertEquals(stage.getId(), draws.get(0).getStageId());
        assertNotNull(draws.get(0).getDrawType());
    }

    @Test
    void shouldGenerateConsolationDrawForConsolationStage() {
        Stage stage = Stage.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .stageNumber(2)
                .stageType(StageType.CONSOLATION)
                .build();

        List<Inscription> inscriptions = List.of(createInscription(), createInscription());

        var draws = drawGenerationService.generateDrawsForStage(stage, inscriptions);

        assertEquals(1, draws.size());
        assertEquals(stage.getId(), draws.get(0).getStageId());
    }

    private Inscription createInscription() {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .participantId(UUID.randomUUID())
                .status("ACTIVE")
                .paymentStatus("PAID")
                .build();
    }
}
