package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.services.StageGenerationService;
import com.tfm.tennis_platform.domain.models.Stage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StageGenerationTest {

    @Autowired
    private StageGenerationService stageGenerationService;

    @Test
    void testSingleEliminationStageGeneration() {
        List<Stage> stages = stageGenerationService.generateStagesForEvent(
                "Male Singles",
                1,
                "MALE",
                "SINGLE_ELIMINATION"
        );

        assertFalse(stages.isEmpty(), "Should generate at least one stage");
        var firstStage = stages.get(0);
        assertNotNull(firstStage.getStageType());
        assertEquals(1, firstStage.getStageNumber());
    }

    @Test
    void testConsolationStageGeneration() {
        List<Stage> stages = stageGenerationService.generateStagesForEvent(
                "Male Singles",
                1,
                "MALE",
                "CONSOLATION"
        );

        assertEquals(2, stages.size(), "Consolation should generate 2 stages (Main + Consolation)");
        assertEquals(1, stages.get(0).getStageNumber());
        assertEquals(2, stages.get(1).getStageNumber());
    }

    @Test
    void testDefaultStrategy() {
        List<Stage> stages = stageGenerationService.generateStagesForEvent(
            "Male Singles",
            1,
            "MALE",
            (String) null
        );

        assertFalse(stages.isEmpty());
        assertEquals(1, stages.get(0).getStageNumber());
    }
}
