package com.puntomatch.application.services.strategies.draw;

import com.puntomatch.domain.models.Draw;
import com.puntomatch.domain.models.Stage;
import com.puntomatch.domain.models.Inscription;

import java.util.List;

public interface DrawGenerationStrategy {
    List<Draw> generateDraws(Stage stage, List<Inscription> inscriptions);
}
