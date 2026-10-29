package com.tfm.tennis_platform.application.services.strategies.draw;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
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
            .label("Ganadores")
            .build();

        Draw losersDraw = Draw.builder()
            .stageId(stage.getId())
            .drawType(DrawType.DOUBLE_ELIMINATION)
            .drawName("Losers Bracket")
            .label("Perdedores")
            .build();

        return List.of(mainDraw, losersDraw);
    }
}
