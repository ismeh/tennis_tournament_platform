package com.tfm.tennis_platform.application.services.strategies.match;

import com.tfm.tennis_platform.domain.models.Draw;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;

import java.util.List;

public interface MatchGenerationStrategy {
    List<Match> generateMatches(Draw draw, List<Inscription> inscriptions);
}
