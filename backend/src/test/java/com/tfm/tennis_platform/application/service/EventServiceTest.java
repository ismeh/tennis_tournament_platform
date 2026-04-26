package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void should_remove_event_when_event_exists_in_tournament() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventIdToKeep = UUID.randomUUID();
        UUID eventIdToRemove = UUID.randomUUID();

        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder().id(eventIdToRemove).categoryId(1).gender("FEMALE").build(),
                        Event.builder().id(eventIdToKeep).categoryId(2).gender("MALE").build()
                ))
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(tournamentRepository.save(org.mockito.ArgumentMatchers.any(Tournament.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Tournament result = eventService.removeEventFromTournament(tournamentId, eventIdToRemove);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());

        assertEquals(1, captor.getValue().getEvents().size());
        assertEquals(eventIdToKeep, captor.getValue().getEvents().get(0).getId());
        assertEquals(1, result.getEvents().size());
    }

    @Test
    void should_throw_when_removing_non_existing_event() {
        UUID tournamentId = UUID.randomUUID();
        UUID existingEventId = UUID.randomUUID();
        UUID missingEventId = UUID.randomUUID();

        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of(
                        Event.builder().id(existingEventId).categoryId(1).gender("FEMALE").build()
                ))
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.removeEventFromTournament(tournamentId, missingEventId)
        );

        assertEquals("Event not found in tournament", exception.getMessage());
    }
}
