package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.services.strategies.stage.ConsolationStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.DoubleEliminationStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.RoundRobinStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.SingleEliminationStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.StageGenerationStrategy;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
        List<Stage> generated = strategy.generateStages(eventName, categoryId, gender);
        return generated.stream()
                .map(s -> s.toBuilder().strategyName(strategyName).build())
                .toList();
    }

    public List<Stage> generateStagesForEvent(String eventName, Integer categoryId, String gender, List<String> strategyNames) {
        if (strategyNames == null || strategyNames.isEmpty()) {
            return generateStagesForEvent(eventName, categoryId, gender, (String) null);
        }

        List<Stage> result = new java.util.ArrayList<>();
        int offset = 0;
        for (String name : strategyNames) {
            String strategyName = determineTournamentType(name);
            StageGenerationStrategy strategy = selectStrategy(strategyName);
            List<Stage> generated = strategy.generateStages(eventName, categoryId, gender);

            boolean alreadyHasMain = result.stream()
                    .anyMatch(s -> StageType.MAIN.equals(s.getStageType()));

            int localCounter = 0;
            for (Stage s : generated) {
                if (alreadyHasMain && StageType.MAIN.equals(s.getStageType())) {
                    continue;
                }
                localCounter++;
                Stage adjusted = s.toBuilder()
                        .stageNumber(offset + localCounter)
                        .strategyName(strategyName)
                        .build();
                result.add(adjusted);
            }
            offset = result.size();
        }
        return result;
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
