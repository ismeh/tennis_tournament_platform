package com.puntomatch.domain.port.out;

import com.puntomatch.domain.models.inscription.EventInscriptionCommand;
import com.puntomatch.domain.models.inscription.EventInscriptionResult;
import com.puntomatch.domain.models.inscription.ManualEventInscriptionCommand;
import com.puntomatch.domain.models.inscription.TournamentInscriptionsView;

import java.util.List;
import java.util.UUID;

public interface InscriptionManagementRepository {
    EventInscriptionResult register(UUID tournamentId, UUID eventId, EventInscriptionCommand request, String requesterEmail);

    EventInscriptionResult registerManual(UUID tournamentId, UUID eventId, ManualEventInscriptionCommand request, String requesterEmail);

    List<EventInscriptionResult> findByEvent(UUID tournamentId, UUID eventId);

    TournamentInscriptionsView findByTournament(UUID tournamentId, UUID eventId);
}
