package com.tfm.tennis_platform.application.strategies.draw;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SingleEliminationDrawGenerator implements DrawGenerationStrategy {

    @Override
    public List<Draw> generateDraws(Stage stage, List<Inscription> inscriptions) {
        Draw mainDraw = Draw.builder()
            .stageId(stage.getId())
            .drawType(DrawType.ELIMINATION)
            .drawName("Main Draw - " + inscriptions.size() + " Players")
            .build();

        return List.of(mainDraw);
    }
}
