package com.tfm.tennis_platform.domain.port.out;

import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionResult;
import com.tfm.tennis_platform.domain.models.inscription.ManualEventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.ParticipantPointsUpdateCommand;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;

import java.util.List;
import java.util.UUID;

public interface InscriptionManagementRepository {
    EventInscriptionResult register(UUID tournamentId, UUID eventId, EventInscriptionCommand request, String requesterEmail);

    EventInscriptionResult registerManual(UUID tournamentId, UUID eventId, ManualEventInscriptionCommand request, String requesterEmail);

    List<EventInscriptionResult> findByEvent(UUID tournamentId, UUID eventId);

    TournamentInscriptionsView findByTournament(UUID tournamentId, UUID eventId);

    void updateParticipantsPoints(UUID tournamentId, List<ParticipantPointsUpdateCommand> updates);
}
