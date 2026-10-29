package com.puntomatch.application.services;

import com.puntomatch.application.services.strategies.match.MatchGenerationStrategy;
import com.puntomatch.application.services.strategies.match.ConsolationMatchGenerator;
import com.puntomatch.application.services.strategies.match.SingleEliminationMatchGenerator;
import com.puntomatch.domain.exceptions.InvalidArgumentException;
import com.puntomatch.domain.models.Draw;
import com.puntomatch.domain.models.Inscription;
import com.puntomatch.domain.models.Match;
import com.puntomatch.domain.models.enums.DrawType;
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
