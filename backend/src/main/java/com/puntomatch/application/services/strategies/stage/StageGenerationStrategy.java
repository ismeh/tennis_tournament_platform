package com.puntomatch.application.services.strategies.stage;

import com.puntomatch.domain.models.Stage;

import java.util.List;

public interface StageGenerationStrategy {
    List<Stage> generateStages(String eventName, Integer categoryId, String gender);
}
