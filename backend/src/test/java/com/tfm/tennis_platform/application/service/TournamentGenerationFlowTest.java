package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.commands.EventCommand;
import com.tfm.tennis_platform.application.services.DrawGenerationService;
import com.tfm.tennis_platform.application.services.EventService;
import com.tfm.tennis_platform.application.services.MatchGenerationService;
import com.tfm.tennis_platform.application.services.MatchPersistenceService;
import com.tfm.tennis_platform.application.services.StageGenerationService;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.application.services.strategies.draw.ConsolationDrawGenerator;
import com.tfm.tennis_platform.application.services.strategies.draw.DoubleEliminationDrawGenerator;
import com.tfm.tennis_platform.application.services.strategies.draw.RoundRobinDrawGenerator;
import com.tfm.tennis_platform.application.services.strategies.draw.SingleEliminationDrawGenerator;
import com.tfm.tennis_platform.application.services.strategies.match.ConsolationMatchGenerator;
import com.tfm.tennis_platform.application.services.strategies.match.DoubleEliminationMatchGenerator;
import com.tfm.tennis_platform.application.services.strategies.match.RoundRobinMatchGenerator;
import com.tfm.tennis_platform.application.services.strategies.match.SingleEliminationMatchGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.ConsolationStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.DoubleEliminationStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.RoundRobinStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.SingleEliminationStageGenerator;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.InscriptionRepository;
import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.ScheduleConfigRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentUmpireRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentGenerationFlowTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private InscriptionRepository inscriptionRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private ScheduleConfigRepository scheduleConfigRepository;

    @Mock
    private TournamentUmpireRepository tournamentUmpireRepository;

    private TournamentService tournamentService;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        StageGenerationService stageGenerationService = new StageGenerationService(
                new SingleEliminationStageGenerator(),
                new RoundRobinStageGenerator(),
                new DoubleEliminationStageGenerator(),
                new ConsolationStageGenerator()
        );
        DrawGenerationService drawGenerationService = new DrawGenerationService(
                new SingleEliminationDrawGenerator(),
                new RoundRobinDrawGenerator(),
                new DoubleEliminationDrawGenerator(),
                new ConsolationDrawGenerator()
        );
        MatchGenerationService matchGenerationService = new MatchGenerationService(new SingleEliminationMatchGenerator(), new ConsolationMatchGenerator(), new RoundRobinMatchGenerator(), new DoubleEliminationMatchGenerator());
        MatchPersistenceService matchPersistenceService = new MatchPersistenceService(matchRepository);

        lenient().when(matchRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        tournamentService = new TournamentService(tournamentRepository, memberRepository, courtRepository, scheduleConfigRepository, tournamentUmpireRepository);
        eventService = new EventService(
                tournamentRepository,
                inscriptionRepository,
                matchRepository,
                courtRepository,
                tournamentService,
                stageGenerationService,
                drawGenerationService,
                matchGenerationService,
                matchPersistenceService
        );
    }

    @Test
    void should_create_tournament_and_generate_events_stages_and_draws() {
        AtomicReference<Tournament> storedTournament = new AtomicReference<>();
        UUID creatorId = UUID.randomUUID();
        Member creator = Member.builder()
                .id(creatorId)
                .email("organizer@example.com")
                .role(UserRole.ORGANIZER)
                .build();

        when(memberRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(creator));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            storedTournament.set(tournament);
            return tournament;
        });
        when(tournamentRepository.findById(any(UUID.class))).thenAnswer(invocation -> Optional.ofNullable(storedTournament.get()));
        UUID firstCourtId = UUID.randomUUID();
        UUID secondCourtId = UUID.randomUUID();
        when(courtRepository.findByTournamentId(any(UUID.class))).thenReturn(List.of(
                Court.builder().id(firstCourtId).name("Pista 1").active(true).build(),
                Court.builder().id(secondCourtId).name("Pista 2").active(true).build()
        ));

        Tournament createdTournament = tournamentService.create(createTournament(), "organizer@example.com");
        assertNotNull(createdTournament.getId());
        assertEquals(creator, createdTournament.getCreatedBy());
        assertTrue(createdTournament.getEvents().isEmpty());

        Tournament tournamentWithEvents = eventService.replaceAllEvents(
                createdTournament.getId(),
                new EventCommand(List.of(new EventCommand.EventItem(UUID.randomUUID(), 1, "MALE", List.of("SINGLE_ELIMINATION")))),
                "organizer@example.com"
        );

        assertEquals(1, tournamentWithEvents.getEvents().size());
        Event event = tournamentWithEvents.getEvents().get(0);
        assertNotNull(event.getId());
        assertFalse(event.getStages().isEmpty());

        // Transition tournament to CLOSED state so draws can be generated
        Tournament closedTournament = storedTournament.get().toBuilder()
                .state(com.tfm.tennis_platform.domain.models.enums.TournamentStatus.CLOSED)
                .build();
        storedTournament.set(closedTournament);

        when(inscriptionRepository.findByEventId(event.getId())).thenReturn(List.of(
                createInscription(event.getId()),
                createInscription(event.getId()),
                createInscription(event.getId()),
                createInscription(event.getId())
        ));

        Tournament tournamentWithDraws = eventService.generateDrawsForEvent(createdTournament.getId(), event.getId(), "organizer@example.com");

        assertEquals(1, tournamentWithDraws.getEvents().size());
        Event generatedEvent = tournamentWithDraws.getEvents().get(0);
        assertFalse(generatedEvent.getStages().isEmpty());

        Stage firstStage = generatedEvent.getStages().get(0);
        assertEquals(1, firstStage.getStageNumber());
        assertFalse(firstStage.getDraws().isEmpty());

        assertEquals(1, tournamentWithDraws.getEvents().size());
        assertEquals(1, tournamentWithDraws.getEvents().get(0).getStages().size());
        assertEquals(1, tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().size());
        assertEquals(3, tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().size());
        assertEquals(
                LocalDateTime.of(2026, 5, 1, 9, 0),
                tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(0).getScheduledAt()
        );
        assertEquals("Pista 1", tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(0).getCourt());
        assertEquals(ScheduleTimeType.EXACT, tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(0).getScheduleTimeType());
        assertEquals(
                LocalDateTime.of(2026, 5, 1, 9, 0),
                tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(1).getScheduledAt()
        );
        assertEquals("Pista 2", tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(1).getCourt());
        assertEquals(ScheduleTimeType.EXACT, tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(1).getScheduleTimeType());
        assertEquals(
                LocalDateTime.of(2026, 5, 1, 10, 0),
                tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(2).getScheduledAt()
        );
        assertEquals("Pista 1", tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(2).getCourt());
        assertEquals(ScheduleTimeType.NOT_BEFORE, tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().get(2).getScheduleTimeType());
    }

    @Test
    void should_generate_consolation_bracket_and_link_main_losers() {
        AtomicReference<Tournament> storedTournament = new AtomicReference<>();
        UUID creatorId = UUID.randomUUID();
        Member creator = Member.builder()
                .id(creatorId)
                .email("organizer@example.com")
                .role(UserRole.ORGANIZER)
                .build();

        when(memberRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(creator));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            storedTournament.set(tournament);
            return tournament;
        });
        when(tournamentRepository.findById(any(UUID.class))).thenAnswer(invocation -> Optional.ofNullable(storedTournament.get()));
        when(courtRepository.findByTournamentId(any(UUID.class))).thenReturn(List.of());

        Tournament createdTournament = tournamentService.create(createTournament(), "organizer@example.com");
        Tournament tournamentWithEvents = eventService.replaceAllEvents(
                createdTournament.getId(),
                new EventCommand(List.of(new EventCommand.EventItem(UUID.randomUUID(), 1, "MALE", List.of("SINGLE_ELIMINATION", "CONSOLATION")))),
                "organizer@example.com"
        );
        Event event = tournamentWithEvents.getEvents().get(0);

        // Transition tournament to CLOSED state so draws can be generated
        Tournament closedTournament = storedTournament.get().toBuilder()
                .state(com.tfm.tennis_platform.domain.models.enums.TournamentStatus.CLOSED)
                .build();
        storedTournament.set(closedTournament);

        when(inscriptionRepository.findByEventId(event.getId())).thenReturn(List.of(
                createInscription(event.getId()),
                createInscription(event.getId()),
                createInscription(event.getId()),
                createInscription(event.getId())
        ));

        Tournament tournamentWithDraws = eventService.generateDrawsForEvent(createdTournament.getId(), event.getId(), "organizer@example.com");
        Event generatedEvent = tournamentWithDraws.getEvents().get(0);

        assertEquals(2, generatedEvent.getStages().size());
        assertEquals(3, generatedEvent.getStages().get(0).getDraws().get(0).getMatches().size());
        assertEquals(1, generatedEvent.getStages().get(1).getDraws().get(0).getMatches().size());

        List<Match> mainFirstRoundMatches = generatedEvent.getStages().get(0).getDraws().get(0).getMatches().stream()
                .filter(match -> match.getRoundNumber() == 1)
                .toList();
        UUID consolationMatchId = generatedEvent.getStages().get(1).getDraws().get(0).getMatches().get(0).getId();

        assertEquals(2, mainFirstRoundMatches.size());
        assertTrue(mainFirstRoundMatches.stream().allMatch(match -> consolationMatchId.equals(match.getLoserNextMatchId())));
    }

    private Tournament createTournament() {
        return Tournament.builder()
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .build();
    }

    private Inscription createInscription(UUID eventId) {
        return Inscription.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .participantId(UUID.randomUUID())
                .status("ACTIVE")
                .paymentStatus("PAID")
                .registeredAt(LocalDateTime.now())
                .build();
    }
}
