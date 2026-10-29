package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.Tournament;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentRepository {
    Tournament save(Tournament tournament);
    List<Tournament> findAll();
    Optional<Tournament> findById(UUID id);
}
