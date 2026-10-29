package com.puntomatch.application.services.strategies.match;

import com.puntomatch.domain.models.Draw;
import com.puntomatch.domain.models.Inscription;
import com.puntomatch.domain.models.Match;

import java.util.List;

public interface MatchGenerationStrategy {
    List<Match> generateMatches(Draw draw, List<Inscription> inscriptions);
}
