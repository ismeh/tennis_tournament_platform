package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.strategies.stage.*;
import com.tfm.tennis_platform.domain.models.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StageGenerationService {

    private final SingleEliminationStageGenerator singleEliminationGenerator;
    private final RoundRobinStageGenerator roundRobinGenerator;
    private final DoubleEliminationStageGenerator doubleEliminationGenerator;
    private final ConsolationStageGenerator consolationGenerator;

    public List<Stage> generateStagesForEvent(String eventName, Integer categoryId, String gender, String tournamentType) {
        String strategyName = determineTournamentType(tournamentType);
        StageGenerationStrategy strategy = selectStrategy(strategyName);
        return strategy.generateStages(eventName, categoryId, gender);
    }

    private StageGenerationStrategy selectStrategy(String strategyName) {
        return switch (strategyName) {
            case "SINGLE_ELIMINATION" -> singleEliminationGenerator;
            case "ROUND_ROBIN" -> roundRobinGenerator;
            case "DOUBLE_ELIMINATION" -> doubleEliminationGenerator;
            case "CONSOLATION" -> consolationGenerator;
            default -> singleEliminationGenerator;
        };
    }

    private String determineTournamentType(String tournamentType) {
        if (tournamentType == null) {
            return "SINGLE_ELIMINATION";
        }
        return tournamentType.toUpperCase();
    }
}
