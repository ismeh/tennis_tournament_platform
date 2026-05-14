package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.commands.EventCommand;
import com.tfm.tennis_platform.application.services.EventService;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.application.services.DrawGenerationService;
import com.tfm.tennis_platform.application.services.StageGenerationService;
import com.tfm.tennis_platform.application.services.strategies.draw.ConsolationDrawGenerator;
import com.tfm.tennis_platform.application.services.strategies.draw.DoubleEliminationDrawGenerator;
import com.tfm.tennis_platform.application.services.strategies.draw.RoundRobinDrawGenerator;
import com.tfm.tennis_platform.application.services.strategies.draw.SingleEliminationDrawGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.ConsolationStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.DoubleEliminationStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.RoundRobinStageGenerator;
import com.tfm.tennis_platform.application.services.strategies.stage.SingleEliminationStageGenerator;
import com.tfm.tennis_platform.domain.models.Event;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Stage;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.port.out.InscriptionRepository;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentGenerationFlowTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private InscriptionRepository inscriptionRepository;

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

        tournamentService = new TournamentService(tournamentRepository, memberRepository);
        eventService = new EventService(tournamentRepository, inscriptionRepository, stageGenerationService, drawGenerationService);
        }

    @Test
    void should_create_tournament_and_generate_events_stages_and_draws() {
        AtomicReference<Tournament> storedTournament = new AtomicReference<>();
        UUID creatorId = UUID.randomUUID();
        Member creator = Member.builder()
                .id(creatorId)
                .email("organizer@example.com")
                .build();

        when(memberRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(creator));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            storedTournament.set(tournament);
            return tournament;
        });
        when(tournamentRepository.findById(any(UUID.class))).thenAnswer(invocation -> Optional.ofNullable(storedTournament.get()));

        Tournament createdTournament = tournamentService.create(createTournament(), "organizer@example.com");
        assertNotNull(createdTournament.getId());
        assertEquals(creator, createdTournament.getCreatedBy());
        assertTrue(createdTournament.getEvents().isEmpty());

        Tournament tournamentWithEvents = eventService.replaceAllEvents(
            createdTournament.getId(),
            new EventCommand(List.of(new EventCommand.EventItem(1, "MALE", null)))
        );

        assertEquals(1, tournamentWithEvents.getEvents().size());
        Event event = tournamentWithEvents.getEvents().get(0);
        assertNotNull(event.getId());
        assertFalse(event.getStages().isEmpty());

        when(inscriptionRepository.findByEventId(event.getId())).thenReturn(List.of(
                createInscription(event.getId()),
                createInscription(event.getId()),
                createInscription(event.getId()),
                createInscription(event.getId())
        ));

        Tournament tournamentWithDraws = eventService.generateDrawsForEvent(createdTournament.getId(), event.getId());

        assertEquals(1, tournamentWithDraws.getEvents().size());
        Event generatedEvent = tournamentWithDraws.getEvents().get(0);
        assertFalse(generatedEvent.getStages().isEmpty());

        Stage firstStage = generatedEvent.getStages().get(0);
        assertEquals(1, firstStage.getStageNumber());
        assertFalse(firstStage.getDraws().isEmpty());

        assertEquals(1, tournamentWithDraws.getEvents().size());
        assertEquals(1, tournamentWithDraws.getEvents().get(0).getStages().size());
        assertEquals(1, tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().size());
        assertTrue(tournamentWithDraws.getEvents().get(0).getStages().get(0).getDraws().get(0).getMatches().isEmpty());
    }

    private Tournament createTournament() {
        return Tournament.builder()
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
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