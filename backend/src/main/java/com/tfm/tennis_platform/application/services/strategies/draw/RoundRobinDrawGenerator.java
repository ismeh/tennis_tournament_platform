package com.tfm.tennis_platform.application.services.strategies.draw;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoundRobinDrawGenerator implements DrawGenerationStrategy {

    private static final int SINGLE_GROUP_MAX_SIZE = 6;
    private static final int DEFAULT_GROUP_SIZE = 4;

    @Override
    public List<Draw> generateDraws(Stage stage, List<Inscription> inscriptions) {
        int playerCount = inscriptions.size();

        if (playerCount <= SINGLE_GROUP_MAX_SIZE) {
            return List.of(Draw.builder()
                .stageId(stage.getId())
                .drawType(DrawType.ROUND_ROBIN)
                .drawName("Grupo unico")
                .build());
        }

        int groupCount = (int) Math.ceil((double) playerCount / DEFAULT_GROUP_SIZE);
        List<Draw> draws = new ArrayList<>();

        for (int i = 0; i < groupCount; i++) {
            draws.add(Draw.builder()
                .stageId(stage.getId())
                .drawType(DrawType.ROUND_ROBIN)
                .drawName("Group " + (char) ('A' + i))
                .groupIndex(i)
                .build());
        }

        return draws;
    }
}
