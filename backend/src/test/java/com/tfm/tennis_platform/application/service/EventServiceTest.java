package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.commands.EventCommand;
import com.tfm.tennis_platform.application.services.DrawGenerationService;
import com.tfm.tennis_platform.application.services.EventService;
import com.tfm.tennis_platform.application.services.MatchGenerationService;
import com.tfm.tennis_platform.application.services.MatchPersistenceService;
import com.tfm.tennis_platform.application.services.StageGenerationService;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.StageType;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.InscriptionRepository;
import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private InscriptionRepository inscriptionRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private TournamentService tournamentService;

    @Mock
    private StageGenerationService stageGenerationService;

    @Mock
    private DrawGenerationService drawGenerationService;

    @Mock
    private MatchGenerationService matchGenerationService;

    @Mock
    private MatchPersistenceService matchPersistenceService;

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
                .startTime(LocalTime.of(9, 0))
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

        Tournament result = eventService.removeEventFromTournament(tournamentId, eventIdToRemove, "organizer@example.com");

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
                .startTime(LocalTime.of(9, 0))
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

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.removeEventFromTournament(tournamentId, missingEventId, "organizer@example.com")
        );

        assertEquals("No se encontró el evento dentro del torneo.", exception.getMessage());
    }

    @Test
    void should_throw_when_tournament_not_found_on_remove_event() {
        UUID tournamentId = UUID.randomUUID();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.removeEventFromTournament(tournamentId, UUID.randomUUID(), "organizer@example.com")
        );
    }

    @Test
    void should_throw_when_tournament_not_in_draft_state_on_remove_event() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.IN_PROGRESS)
                .events(List.of(
                        Event.builder().id(UUID.randomUUID()).categoryId(1).gender("FEMALE").build()
                ))
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> eventService.removeEventFromTournament(tournamentId, UUID.randomUUID(), "organizer@example.com")
        );

        assertEquals("Solo se pueden añadir o eliminar eventos en torneos en borrador (DRAFT). Estado actual: IN_PROGRESS", exception.getMessage());
    }

    @Test
    void should_replace_all_events_when_tournament_is_in_draft() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of())
                .build();

        EventCommand eventCommand = new EventCommand(List.of(
                new EventCommand.EventItem(null, 1, "MALE", List.of())
        ));

        List<Stage> generatedStages = List.of(
                Stage.builder().stageNumber(1).stageType(StageType.MAIN).build()
        );

        Event generatedEvent = Event.builder()
                .tournamentId(tournamentId)
                .categoryId(1)
                .gender("MALE")
                .stages(generatedStages)
                .build();

        Tournament updatedTournament = tournament.toBuilder()
                .events(List.of(generatedEvent))
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(stageGenerationService.generateStagesForEvent(any(), any(Integer.class), any(String.class), anyList()))
                .thenReturn(generatedStages);
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(updatedTournament);

        Tournament result = eventService.replaceAllEvents(tournamentId, eventCommand, "organizer@example.com");

        verify(tournamentRepository).save(any(Tournament.class));
        assertNotNull(result);
    }

    @Test
    void should_throw_when_tournament_not_found_on_replace_events() {
        UUID tournamentId = UUID.randomUUID();
        EventCommand eventCommand = new EventCommand(List.of(
                new EventCommand.EventItem(null, 1, "MALE", List.of())
        ));

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.replaceAllEvents(tournamentId, eventCommand, "organizer@example.com")
        );
    }

    @Test
    void should_throw_when_tournament_not_in_draft_state_on_replace_events() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.IN_PROGRESS)
                .events(List.of())
                .build();

        EventCommand eventCommand = new EventCommand(List.of(
                new EventCommand.EventItem(null, 1, "MALE", List.of())
        ));

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        assertThrows(
                InvalidArgumentException.class,
                () -> eventService.replaceAllEvents(tournamentId, eventCommand, "organizer@example.com")
        );
    }

    @Test
    void should_generate_draws_for_event_when_tournament_is_closed() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID drawId = UUID.randomUUID();

        Stage stage = Stage.builder()
                .id(stageId)
                .eventId(eventId)
                .stageNumber(1)
                .stageType(StageType.MAIN)
                .build();

        Event event = Event.builder()
                .id(eventId)
                .tournamentId(tournamentId)
                .categoryId(1)
                .gender("MALE")
                .stages(List.of(stage))
                .build();

        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.CLOSED)
                .events(List.of(event))
                .build();

        Inscription inscription = Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .participantId(UUID.randomUUID())
                .build();

        com.tfm.tennis_platform.domain.models.Draw generatedDraw = com.tfm.tennis_platform.domain.models.Draw.builder()
                .id(drawId)
                .stageId(stageId)
                .drawType(com.tfm.tennis_platform.domain.models.enums.DrawType.ELIMINATION)
                .drawName("Main Draw")
                .label("Main Draw")
                .build();

        Stage updatedStage = stage.toBuilder()
                .draws(List.of(generatedDraw))
                .build();

        Event updatedEvent = event.toBuilder()
                .stages(List.of(updatedStage))
                .build();

        Tournament updatedTournament = tournament.toBuilder()
                .events(List.of(updatedEvent))
                .build();

        Court court = Court.builder()
                .id(UUID.randomUUID())
                .tournamentId(tournamentId)
                .name("Pista 1")
                .active(true)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(inscriptionRepository.findByEventId(eventId)).thenReturn(List.of(inscription));
        when(drawGenerationService.generateDrawsForStage(any(Stage.class), any())).thenReturn(List.of(generatedDraw));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(updatedTournament);
        when(courtRepository.findByTournamentId(tournamentId)).thenReturn(List.of(court));
        when(matchGenerationService.generateMatchesForDraw(any(), any())).thenReturn(List.of());
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(updatedTournament));

        Tournament result = eventService.generateDrawsForEvent(tournamentId, eventId, "organizer@example.com");

        assertNotNull(result);
        verify(tournamentRepository).save(any(Tournament.class));
    }

    @Test
    void should_generate_draws_when_tournament_is_in_progress() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        Stage stage = Stage.builder()
                .id(stageId)
                .eventId(eventId)
                .stageNumber(1)
                .stageType(StageType.MAIN)
                .build();

        Event event = Event.builder()
                .id(eventId)
                .tournamentId(tournamentId)
                .categoryId(1)
                .gender("MALE")
                .stages(List.of(stage))
                .build();

        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.IN_PROGRESS)
                .events(List.of(event))
                .build();

        Inscription inscription = Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .participantId(UUID.randomUUID())
                .build();

        com.tfm.tennis_platform.domain.models.Draw generatedDraw = com.tfm.tennis_platform.domain.models.Draw.builder()
                .stageId(stageId)
                .drawType(com.tfm.tennis_platform.domain.models.enums.DrawType.ELIMINATION)
                .drawName("Main Draw")
                .label("Main Draw")
                .build();

        Stage updatedStage = stage.toBuilder()
                .draws(List.of(generatedDraw))
                .build();

        Event updatedEvent = event.toBuilder()
                .stages(List.of(updatedStage))
                .build();

        Tournament updatedTournament = tournament.toBuilder()
                .events(List.of(updatedEvent))
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(inscriptionRepository.findByEventId(eventId)).thenReturn(List.of(inscription));
        when(drawGenerationService.generateDrawsForStage(any(Stage.class), any())).thenReturn(List.of(generatedDraw));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(updatedTournament);
        when(courtRepository.findByTournamentId(tournamentId)).thenReturn(List.of());
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(updatedTournament));

        Tournament result = eventService.generateDrawsForEvent(tournamentId, eventId, "organizer@example.com");

        assertNotNull(result);
    }

    @Test
    void should_throw_when_tournament_not_found_on_generate_draws() {
        UUID tournamentId = UUID.randomUUID();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.generateDrawsForEvent(tournamentId, UUID.randomUUID(), "organizer@example.com")
        );
    }

    @Test
    void should_throw_when_tournament_not_in_valid_state_for_draws() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> eventService.generateDrawsForEvent(tournamentId, UUID.randomUUID(), "organizer@example.com")
        );

        assertEquals("Solo se pueden generar cuadros en torneos cerrados o en curso. Estado actual: DRAFT", exception.getMessage());
    }

    @Test
    void should_throw_when_event_not_found_in_tournament_on_generate_draws() {
        UUID tournamentId = UUID.randomUUID();
        UUID missingEventId = UUID.randomUUID();

        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.CLOSED)
                .events(List.of(
                        Event.builder().id(UUID.randomUUID()).categoryId(1).gender("MALE").build()
                ))
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> eventService.generateDrawsForEvent(tournamentId, missingEventId, "organizer@example.com")
        );

        assertEquals("No se encontró el evento dentro del torneo.", exception.getMessage());
    }

    @Test
    void should_throw_when_event_has_no_inscriptions_on_generate_draws() {
        UUID tournamentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.CLOSED)
                .events(List.of(
                        Event.builder().id(eventId).categoryId(1).gender("MALE").build()
                ))
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(inscriptionRepository.findByEventId(eventId)).thenReturn(List.of());

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> eventService.generateDrawsForEvent(tournamentId, eventId, "organizer@example.com")
        );

        assertEquals("Este evento todavía no tiene jugadores inscritos.", exception.getMessage());
    }

    @Test
    void should_throw_when_tournament_not_in_editable_state_on_remove_event_closed() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.CLOSED)
                .events(List.of(
                        Event.builder().id(UUID.randomUUID()).categoryId(1).gender("FEMALE").build()
                ))
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> eventService.removeEventFromTournament(tournamentId, UUID.randomUUID(), "organizer@example.com")
        );

        assertTrue(exception.getMessage().contains("Solo se pueden añadir o eliminar eventos"));
    }

    @Test
    void should_throw_when_tournament_not_in_editable_state_on_replace_events_open() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.OPEN)
                .events(List.of())
                .build();

        EventCommand eventCommand = new EventCommand(List.of(
                new EventCommand.EventItem(null, 1, "MALE", List.of())
        ));

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> eventService.replaceAllEvents(tournamentId, eventCommand, "organizer@example.com")
        );

        assertTrue(exception.getMessage().contains("Solo se pueden añadir o eliminar eventos"));
    }

    private static void assertNotNull(Object value) {
        org.junit.jupiter.api.Assertions.assertNotNull(value);
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
