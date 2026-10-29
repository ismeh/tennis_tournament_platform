package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.Match;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchRepository {
    Match save(Match match);
    List<Match> saveAll(List<Match> matches);
    List<Match> findByTournamentId(UUID tournamentId);
    Optional<Match> findById(String id);
}
