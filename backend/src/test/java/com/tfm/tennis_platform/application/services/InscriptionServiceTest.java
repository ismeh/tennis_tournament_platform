package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.EventInscriptionResult;
import com.tfm.tennis_platform.domain.models.inscription.ManualEventInscriptionCommand;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;
import com.tfm.tennis_platform.domain.models.enums.ParticipantSource;
import com.tfm.tennis_platform.domain.port.out.InscriptionManagementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscriptionServiceTest {

    @Mock
    private InscriptionManagementRepository inscriptionManagementRepository;

    @InjectMocks
    private InscriptionService inscriptionService;

    @Test
    void registerShouldDelegateToPort() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        String requesterEmail = "player@example.com";

        EventInscriptionCommand command = new EventInscriptionCommand(1, null);
        EventInscriptionResult expected = new EventInscriptionResult(
                UUID.randomUUID(),
                eventId,
                participantId,
                "PENDING",
                "UNPAID",
                LocalDateTime.now()
        );

        when(inscriptionManagementRepository.register(tournamentId, eventId, command, requesterEmail)).thenReturn(expected);

        EventInscriptionResult actual = inscriptionService.register(tournamentId, eventId, command, requesterEmail);

        assertEquals(expected, actual);
        verify(inscriptionManagementRepository).register(tournamentId, eventId, command, requesterEmail);
    }

    @Test
    void registerManualShouldDelegateToPort() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String requesterEmail = "admin@example.com";

        ManualEventInscriptionCommand command = new ManualEventInscriptionCommand(
                ParticipantSource.MANUAL,
                null,
                "Lucia",
                "Perez",
                "FEMALE",
                LocalDate.of(1998, 3, 5),
                "ESP",
                "LIC-77",
                null
        );

        EventInscriptionResult expected = new EventInscriptionResult(
                UUID.randomUUID(),
                eventId,
                UUID.randomUUID(),
                "PENDING",
                "UNPAID",
                LocalDateTime.now()
        );

        when(inscriptionManagementRepository.registerManual(tournamentId, eventId, command, requesterEmail)).thenReturn(expected);

        EventInscriptionResult actual = inscriptionService.registerManual(tournamentId, eventId, command, requesterEmail);

        assertEquals(expected, actual);
        verify(inscriptionManagementRepository).registerManual(tournamentId, eventId, command, requesterEmail);
    }

    @Test
    void findByEventShouldDelegateToPort() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        List<EventInscriptionResult> expected = List.of(
                new EventInscriptionResult(UUID.randomUUID(), eventId, UUID.randomUUID(), "PENDING", "UNPAID", LocalDateTime.now())
        );

        when(inscriptionManagementRepository.findByEvent(tournamentId, eventId)).thenReturn(expected);

        List<EventInscriptionResult> actual = inscriptionService.findByEvent(tournamentId, eventId);

        assertEquals(expected, actual);
        verify(inscriptionManagementRepository).findByEvent(tournamentId, eventId);
    }

    @Test
    void findByTournamentShouldDelegateToPort() {
        UUID tournamentId = UUID.randomUUID();
        TournamentInscriptionsView expected = new TournamentInscriptionsView(tournamentId, null, List.of(), List.of(), List.of());

        when(inscriptionManagementRepository.findByTournament(tournamentId, null)).thenReturn(expected);

        TournamentInscriptionsView actual = inscriptionService.findByTournament(tournamentId, null);

        assertEquals(expected, actual);
        verify(inscriptionManagementRepository).findByTournament(tournamentId, null);
    }
}
