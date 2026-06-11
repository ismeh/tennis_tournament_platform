package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentRepository {
    Tournament save(Tournament tournament);
    List<Tournament> findAll();
    List<TournamentSummary> findSummaries();
    Optional<Tournament> findById(UUID id);
    boolean isProfessionalTournament(UUID id);
}
