package com.puntomatch.application.services.strategies.stage;

import com.puntomatch.domain.models.Stage;
import com.puntomatch.domain.models.enums.StageType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoundRobinStageGenerator implements StageGenerationStrategy {
    @Override
    public List<Stage> generateStages(String eventName, Integer categoryId, String gender) {
        return List.of(
            Stage.builder()
                .stageNumber(1)
                .stageType(StageType.MAIN)
                .build()
        );
    }
}
