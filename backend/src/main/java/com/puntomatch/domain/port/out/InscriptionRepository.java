package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.Inscription;
import java.util.List;
import java.util.UUID;

public interface InscriptionRepository {
    Inscription save(Inscription inscription);
    List<Inscription> findByTournamentId(UUID tournamentId);
    List<Inscription> findByEventId(UUID eventId);
}
