package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.ScheduleConfigRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private ScheduleConfigRepository scheduleConfigRepository;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void should_create_tournament_with_authenticated_creator() {
        UUID creatorId = UUID.randomUUID();
        Member creator = Member.builder()
                .id(creatorId)
                .email("organizer@example.com")
                .role(UserRole.ORGANIZER)
                .build();
        Tournament incomingTournament = Tournament.builder()
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .build();
        Tournament persistedTournament = Tournament.builder()
                .id(UUID.randomUUID())
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(creator)
                .events(List.of())
                .build();

        when(memberRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(creator));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(persistedTournament);

        Tournament result = tournamentService.create(incomingTournament, "organizer@example.com");

        ArgumentCaptor<Tournament> tournamentCaptor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(tournamentCaptor.capture());

                assertEquals(creator, tournamentCaptor.getValue().getCreatedBy());
        assertEquals("Open de Primavera", result.getName());
                assertEquals(creator, result.getCreatedBy());
    }

    @Test
    void should_throw_when_creator_email_is_unknown() {
        Tournament incomingTournament = Tournament.builder()
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .build();

        when(memberRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tournamentService.create(incomingTournament, "missing@example.com")
        );

        assertEquals("No se encontró la cuenta solicitada.", exception.getMessage());
    }

    @Test
    void should_find_tournament_summaries() {
        TournamentSummary tournamentSummary = new TournamentSummary(
                UUID.randomUUID(),
                "Open de Primavera",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 20),
                Surface.CLAY,
                32,
                "Club Central",
                TournamentStatus.OPEN,
                true
        );
        when(tournamentRepository.findSummaries()).thenReturn(List.of(tournamentSummary));

        List<TournamentSummary> result = tournamentService.findSummaries();

        assertEquals(List.of(tournamentSummary), result);
        verify(tournamentRepository).findSummaries();
    }

    @Test
    void should_allow_tournament_admin_when_tournament_has_only_creator_id() {
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = createPersistedTournamentWithCreator(Member.builder().id(creatorId).build());

        when(memberRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("organizer@example.com")
                .build()));

        assertDoesNotThrow(() -> tournamentService.assertTournamentAdmin(tournament, "organizer@example.com"));
    }

    @Test
    void should_deny_tournament_admin_when_creator_id_is_different() {
        Tournament tournament = createPersistedTournamentWithCreator(Member.builder().id(UUID.randomUUID()).build());

        when(memberRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(Member.builder()
                .id(UUID.randomUUID())
                .email("organizer@example.com")
                .build()));

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.assertTournamentAdmin(tournament, "organizer@example.com")
        );
    }

    private Tournament createPersistedTournamentWithCreator(Member creator) {
        return Tournament.builder()
                .id(UUID.randomUUID())
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(creator)
                .events(List.of())
                .build();
    }
}
