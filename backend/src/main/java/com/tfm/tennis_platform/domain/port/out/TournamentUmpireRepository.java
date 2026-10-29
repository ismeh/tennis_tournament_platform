package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.TournamentUmpire;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentUmpireRepository {
    TournamentUmpire save(TournamentUmpire tournamentUmpire);
    List<TournamentUmpire> findByTournamentId(UUID tournamentId);
    Optional<TournamentUmpire> findByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId);
    boolean existsByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId);
    void deleteByTournamentIdAndUmpireId(UUID tournamentId, UUID umpireId);
}
