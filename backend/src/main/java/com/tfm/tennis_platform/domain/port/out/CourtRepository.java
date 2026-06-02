package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Court;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtRepository {
    Court save(Court court);
    List<Court> findByTournamentId(UUID tournamentId);
    Optional<Court> findByIdAndTournamentId(UUID id, UUID tournamentId);
    boolean existsByTournamentIdAndName(UUID tournamentId, String name);
    void deleteById(UUID id);
}
