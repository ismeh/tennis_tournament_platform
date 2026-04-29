package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.Inscription;
import java.util.List;
import java.util.UUID;

public interface InscriptionRepository {
    Inscription save(Inscription inscription);
    List<Inscription> findByTournamentId(UUID tournamentId);
}
