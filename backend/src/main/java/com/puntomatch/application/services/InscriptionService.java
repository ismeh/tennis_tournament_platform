package com.puntomatch.application.services;

import com.puntomatch.domain.models.inscription.EventInscriptionCommand;
import com.puntomatch.domain.models.inscription.EventInscriptionResult;
import com.puntomatch.domain.models.inscription.ManualEventInscriptionCommand;
import com.puntomatch.domain.models.inscription.TournamentInscriptionsView;
import com.puntomatch.domain.port.out.InscriptionManagementRepository;
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
}
