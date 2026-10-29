package com.puntomatch.application.services;

import com.puntomatch.application.services.strategies.draw.ConsolationDrawGenerator;
import com.puntomatch.application.services.strategies.draw.DoubleEliminationDrawGenerator;
import com.puntomatch.application.services.strategies.draw.DrawGenerationStrategy;
import com.puntomatch.application.services.strategies.draw.RoundRobinDrawGenerator;
import com.puntomatch.application.services.strategies.draw.SingleEliminationDrawGenerator;
import com.puntomatch.domain.models.Draw;
import com.puntomatch.domain.models.Inscription;
import com.puntomatch.domain.models.Stage;
import com.puntomatch.domain.models.enums.StageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DrawGenerationService {

    private final SingleEliminationDrawGenerator singleEliminationDrawGenerator;
    private final RoundRobinDrawGenerator roundRobinDrawGenerator;
    private final DoubleEliminationDrawGenerator doubleEliminationDrawGenerator;
    private final ConsolationDrawGenerator consolationDrawGenerator;

    public List<Draw> generateDrawsForStages(List<Stage> stages, List<Inscription> inscriptions) {
        List<Draw> allDraws = new ArrayList<>();

        for (Stage stage : stages) {
            DrawGenerationStrategy strategy = selectStrategy(stage.getStageType());
            List<Draw> stageDraws = strategy.generateDraws(stage, inscriptions);
            allDraws.addAll(stageDraws);
        }

        return allDraws;
    }

    public List<Draw> generateDrawsForStage(Stage stage, List<Inscription> inscriptions) {
        DrawGenerationStrategy strategy = selectStrategy(stage.getStageType());
        return strategy.generateDraws(stage, inscriptions);
    }

    private DrawGenerationStrategy selectStrategy(StageType stageType) {
        return switch (stageType) {
            case MAIN -> singleEliminationDrawGenerator;
            case QUALIFYING -> singleEliminationDrawGenerator;
            case CONSOLATION -> consolationDrawGenerator;
            case PLAYOFF -> doubleEliminationDrawGenerator;
        };
    }
}
