package com.tfm.tennis_platform.application.strategies.stage;

import com.tfm.tennis_platform.domain.models.Stage;

import java.util.List;

public interface StageGenerationStrategy {
    List<Stage> generateStages(String eventName, Integer categoryId, String gender);
}
