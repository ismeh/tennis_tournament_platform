package com.puntomatch.application.services.strategies.draw;

import com.puntomatch.domain.models.Draw;
import com.puntomatch.domain.models.Inscription;
import com.puntomatch.domain.models.Stage;
import com.puntomatch.domain.models.enums.DrawType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DoubleEliminationDrawGenerator implements DrawGenerationStrategy {

    @Override
    public List<Draw> generateDraws(Stage stage, List<Inscription> inscriptions) {
        Draw mainDraw = Draw.builder()
            .stageId(stage.getId())
            .drawType(DrawType.ELIMINATION)
            .drawName("Winners Bracket - " + inscriptions.size() + " Players")
            .build();

        Draw consolationDraw = Draw.builder()
            .stageId(stage.getId())
            .drawType(DrawType.CONSOLATION)
            .drawName("Losers Bracket")
            .build();

        return List.of(mainDraw, consolationDraw);
    }
}
