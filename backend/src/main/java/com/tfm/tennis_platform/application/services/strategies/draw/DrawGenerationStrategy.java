package com.tfm.tennis_platform.application.strategies.draw;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Inscription;

import java.util.List;

public interface DrawGenerationStrategy {
    List<Draw> generateDraws(Stage stage, List<Inscription> inscriptions);
}
