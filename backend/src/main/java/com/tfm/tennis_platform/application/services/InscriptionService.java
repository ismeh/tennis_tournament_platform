package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionResult;
import com.tfm.tennis_platform.domain.models.inscription.ManualEventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.ParticipantDetailUpdateCommand;
import com.tfm.tennis_platform.domain.models.inscription.ParticipantPointsUpdateCommand;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;
import com.tfm.tennis_platform.domain.port.out.InscriptionManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final InscriptionManagementRepository inscriptionManagementRepository;

    public EventInscriptionResult register(
            UUID tournamentId,
            UUID eventId,
            EventInscriptionCommand request,
            String requesterEmail
    ) {
        return inscriptionManagementRepository.register(tournamentId, eventId, request, requesterEmail);
    }

    public EventInscriptionResult registerManual(
            UUID tournamentId,
            UUID eventId,
            ManualEventInscriptionCommand request,
            String requesterEmail
    ) {
        return inscriptionManagementRepository.registerManual(tournamentId, eventId, request, requesterEmail);
    }

    public List<EventInscriptionResult> findByEvent(UUID tournamentId, UUID eventId) {
        return inscriptionManagementRepository.findByEvent(tournamentId, eventId);
    }

    public TournamentInscriptionsView findByTournament(UUID tournamentId, UUID eventId) {
        return inscriptionManagementRepository.findByTournament(tournamentId, eventId);
    }

    public void updateParticipantsPoints(UUID tournamentId, List<ParticipantPointsUpdateCommand> updates, String requesterEmail) {
        inscriptionManagementRepository.updateParticipantsPoints(tournamentId, updates);
    }

    public void updateParticipantDetails(UUID tournamentId, ParticipantDetailUpdateCommand update) {
        inscriptionManagementRepository.updateParticipantDetails(tournamentId, update);
    }
}
