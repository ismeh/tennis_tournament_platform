package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.application.services.strategies.match.MatchGenerationStrategy;
import com.tfm.tennis_platform.application.services.strategies.match.ConsolationMatchGenerator;
import com.tfm.tennis_platform.application.services.strategies.match.SingleEliminationMatchGenerator;
import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.enums.DrawType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchGenerationService {

    private final SingleEliminationMatchGenerator singleEliminationMatchGenerator;
    private final ConsolationMatchGenerator consolationMatchGenerator;

    public List<Match> generateMatchesForDraw(Draw draw, List<Inscription> inscriptions) {
        MatchGenerationStrategy strategy = selectStrategy(draw.getDrawType());
            return strategy.generateMatches(draw, inscriptions);
    }

    private MatchGenerationStrategy selectStrategy(DrawType drawType) {
        return switch (drawType) {
            case ELIMINATION -> singleEliminationMatchGenerator;
            case CONSOLATION -> consolationMatchGenerator;
            // TODO: Implement other strategies
            // case ROUND_ROBIN -> roundRobinMatchGenerator;
            // case DOUBLE_ELIMINATION -> doubleEliminationMatchGenerator;
            default -> throw new InvalidArgumentException("El tipo de cuadro seleccionado todavía no está disponible.");
        };
    }
}
