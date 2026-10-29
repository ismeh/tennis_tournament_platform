package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Mock
    private com.tfm.tennis_platform.domain.port.out.TournamentUmpireRepository tournamentUmpireRepository;

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
    void should_throw_when_creator_is_not_organizer() {
        UUID creatorId = UUID.randomUUID();
        Member player = Member.builder()
                .id(creatorId)
                .email("player@example.com")
                .role(UserRole.PLAYER)
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

        when(memberRepository.findByEmail("player@example.com")).thenReturn(Optional.of(player));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.create(incomingTournament, "player@example.com")
        );

        assertEquals("Only organizers can create tournaments.", exception.getMessage());
    }

    @Test
    void should_throw_when_start_time_is_null() {
        UUID creatorId = UUID.randomUUID();
        Member creator = Member.builder()
                .id(creatorId)
                .email("organizer@example.com")
                .role(UserRole.ORGANIZER)
                .build();
        Tournament incomingTournament = Tournament.builder()
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(null)
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .build();

        when(memberRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(creator));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> tournamentService.create(incomingTournament, "organizer@example.com")
        );

        assertEquals("La hora de inicio del torneo es obligatoria.", exception.getMessage());
    }

    @Test
    void should_create_initial_courts_when_court_count_positive() {
        UUID creatorId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
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
                .id(tournamentId)
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
        when(courtRepository.existsByTournamentIdAndName(any(UUID.class), any(String.class))).thenReturn(false);

        tournamentService.create(incomingTournament, "organizer@example.com", 2);

        ArgumentCaptor<Court> courtCaptor = ArgumentCaptor.forClass(Court.class);
        verify(courtRepository, org.mockito.Mockito.times(2)).save(courtCaptor.capture());

        List<Court> savedCourts = courtCaptor.getAllValues();
        assertEquals(2, savedCourts.size());
        assertEquals("Pista 1", savedCourts.get(0).getName());
        assertEquals("Pista 2", savedCourts.get(1).getName());
        assertTrue(savedCourts.get(0).isActive());
    }

    @Test
    void should_skip_court_creation_when_name_already_exists() {
        UUID creatorId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
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
                .id(tournamentId)
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
        when(courtRepository.existsByTournamentIdAndName(any(UUID.class), any(String.class))).thenReturn(true);

        tournamentService.create(incomingTournament, "organizer@example.com", 1);

        verify(courtRepository, never()).save(any(Court.class));
    }

    @Test
    void should_throw_when_court_count_is_negative() {
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

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> tournamentService.create(incomingTournament, "organizer@example.com", -1)
        );

        assertEquals("El número de pistas no puede ser negativo.", exception.getMessage());
    }

    @Test
    void should_create_default_schedule_config_on_tournament_creation() {
        UUID creatorId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
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
                .id(tournamentId)
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

        tournamentService.create(incomingTournament, "organizer@example.com");

        ArgumentCaptor<ScheduleConfig> configCaptor = ArgumentCaptor.forClass(ScheduleConfig.class);
        verify(scheduleConfigRepository).save(configCaptor.capture());

        ScheduleConfig savedConfig = configCaptor.getValue();
        assertEquals(tournamentId, savedConfig.getTournamentId());
        assertEquals(60, savedConfig.getMatchDurationMinutes());
        assertNotNull(savedConfig.getTimeSlots());
        assertEquals(2, savedConfig.getTimeSlots().size());
    }

    @Test
    void should_delegate_two_arg_create_to_three_arg_with_zero_courts() {
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

        tournamentService.create(incomingTournament, "organizer@example.com");

        verify(courtRepository, never()).save(any(Court.class));
    }

    @Test
    void should_return_all_tournaments() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .events(List.of())
                .build();

        when(tournamentRepository.findAll()).thenReturn(List.of(tournament));

        List<Tournament> result = tournamentService.findAll();

        assertEquals(1, result.size());
        assertEquals(tournamentId, result.get(0).getId());
        verify(tournamentRepository).findAll();
    }

    @Test
    void should_find_tournament_by_id() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
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

        Optional<Tournament> result = tournamentService.findById(tournamentId);

        assertTrue(result.isPresent());
        assertEquals(tournamentId, result.get().getId());
    }

    @Test
    void should_return_empty_when_tournament_not_found_by_id() {
        UUID tournamentId = UUID.randomUUID();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        Optional<Tournament> result = tournamentService.findById(tournamentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void should_check_if_tournament_is_professional() {
        UUID tournamentId = UUID.randomUUID();

        when(tournamentRepository.isProfessionalTournament(tournamentId)).thenReturn(true);

        boolean result = tournamentService.isProfessionalTournament(tournamentId);

        assertTrue(result);
        verify(tournamentRepository).isProfessionalTournament(tournamentId);
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

    @Test
    void should_allow_umpire_as_tournament_admin() {
        UUID tournamentAdminId = UUID.randomUUID();
        Tournament tournament = createPersistedTournamentWithCreator(Member.builder().id(tournamentAdminId).build());

        UUID umpireId = UUID.randomUUID();
        when(memberRepository.findByEmail("umpire@example.com")).thenReturn(Optional.of(Member.builder()
                .id(umpireId)
                .email("umpire@example.com")
                .role(UserRole.UMPIRE)
                .build()));
        when(tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournament.getId(), umpireId)).thenReturn(true);

        assertDoesNotThrow(() -> tournamentService.assertTournamentAdmin(tournament, "umpire@example.com"));
    }

    @Test
    void should_throw_when_tournament_is_null_in_admin_check() {
        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.assertTournamentAdmin((Tournament) null, "organizer@example.com")
        );
    }

    @Test
    void should_throw_when_created_by_is_null_in_admin_check() {
        Tournament tournament = Tournament.builder()
                .id(UUID.randomUUID())
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(null)
                .events(List.of())
                .build();

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.assertTournamentAdmin(tournament, "organizer@example.com")
        );
    }

    @Test
    void should_throw_when_requester_email_is_null_in_admin_check() {
        Tournament tournament = createPersistedTournamentWithCreator(Member.builder().id(UUID.randomUUID()).build());

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.assertTournamentAdmin(tournament, null)
        );
    }

    @Test
    void should_throw_when_requester_email_not_found_in_admin_check() {
        UUID tournamentAdminId = UUID.randomUUID();
        Tournament tournament = createPersistedTournamentWithCreator(Member.builder().id(tournamentAdminId).build());

        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.assertTournamentAdmin(tournament, "unknown@example.com")
        );
    }

    @Test
    void should_allow_admin_when_email_matches_but_ids_differ() {
        UUID tournamentAdminId = UUID.randomUUID();
        Member tournamentCreator = Member.builder()
                .id(tournamentAdminId)
                .email("admin@example.com")
                .build();
        Tournament tournament = createPersistedTournamentWithCreator(tournamentCreator);

        Member requester = Member.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .role(UserRole.ORGANIZER)
                .build();

        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(requester));

        assertDoesNotThrow(() -> tournamentService.assertTournamentAdmin(tournament, "admin@example.com"));
    }

    @Test
    void should_throw_when_admin_email_differs_from_requester() {
        UUID tournamentAdminId = UUID.randomUUID();
        Member tournamentCreator = Member.builder()
                .id(tournamentAdminId)
                .email("admin@example.com")
                .build();
        Tournament tournament = createPersistedTournamentWithCreator(tournamentCreator);

        Member requester = Member.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .role(UserRole.ORGANIZER)
                .build();

        when(memberRepository.findByEmail("other@example.com")).thenReturn(Optional.of(requester));

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.assertTournamentAdmin(tournament, "other@example.com")
        );
    }

    @Test
    void should_throw_when_admin_email_is_null_and_ids_differ() {
        UUID tournamentAdminId = UUID.randomUUID();
        Member tournamentCreator = Member.builder()
                .id(tournamentAdminId)
                .email(null)
                .build();
        Tournament tournament = createPersistedTournamentWithCreator(tournamentCreator);

        Member requester = Member.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .role(UserRole.ORGANIZER)
                .build();

        when(memberRepository.findByEmail("other@example.com")).thenReturn(Optional.of(requester));

        assertThrows(
                AccessDeniedException.class,
                () -> tournamentService.assertTournamentAdmin(tournament, "other@example.com")
        );
    }

    @Test
    void should_use_uuid_overload_of_assert_tournament_admin() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = createPersistedTournamentWithCreator(Member.builder().id(creatorId).build());

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("organizer@example.com")
                .build()));

        assertDoesNotThrow(() -> tournamentService.assertTournamentAdmin(tournamentId, "organizer@example.com"));
    }

    @Test
    void should_throw_when_tournament_not_found_in_uuid_admin_check() {
        UUID tournamentId = UUID.randomUUID();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> tournamentService.assertTournamentAdmin(tournamentId, "organizer@example.com")
        );
    }

    @Test
    void should_update_status_successfully() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();
        Tournament updatedTournament = tournament.toBuilder().state(TournamentStatus.OPEN).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(updatedTournament);

        Tournament result = tournamentService.updateStatus(tournamentId, TournamentStatus.OPEN, "admin@example.com");

        assertEquals(TournamentStatus.OPEN, result.getState());
        verify(tournamentRepository).save(any(Tournament.class));
    }

    @Test
    void should_throw_when_tournament_not_found_on_status_update() {
        UUID tournamentId = UUID.randomUUID();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> tournamentService.updateStatus(tournamentId, TournamentStatus.OPEN, "admin@example.com")
        );
    }

    @Test
    void should_throw_when_new_status_is_null_on_status_update() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> tournamentService.updateStatus(tournamentId, null, "admin@example.com")
        );

        assertEquals("Selecciona un estado válido para el torneo.", exception.getMessage());
    }

    @Test
    void should_return_same_tournament_when_status_is_unchanged() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));

        Tournament result = tournamentService.updateStatus(tournamentId, TournamentStatus.DRAFT, "admin@example.com");

        assertSame(tournament, result);
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }

    @Test
    void should_allow_any_status_transition_for_corrections() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();
        Tournament savedTournament = tournament.toBuilder()
                .state(TournamentStatus.COMPLETED)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);

        Tournament result = tournamentService.updateStatus(tournamentId, TournamentStatus.COMPLETED, "admin@example.com");

        assertEquals(TournamentStatus.COMPLETED, result.getState());
        verify(tournamentRepository).save(any(Tournament.class));
    }

    @Test
    void should_allow_reverse_transition_open_to_draft() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.OPEN)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();
        Tournament savedTournament = tournament.toBuilder()
                .state(TournamentStatus.DRAFT)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);

        Tournament result = tournamentService.updateStatus(tournamentId, TournamentStatus.DRAFT, "admin@example.com");

        assertEquals(TournamentStatus.DRAFT, result.getState());
        verify(tournamentRepository).save(any(Tournament.class));
    }

    @Test
    void should_update_general_info_in_draft_status() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();
        Tournament savedTournament = tournament.toBuilder()
                .name("Updated Name")
                .maxPlayers(64)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);

        Tournament result = tournamentService.updateGeneralInfo(
                tournamentId,
                "Updated Name",
                null,
                null,
                null,
                null,
                64,
                null,
                null,
                null,
                null,
                null,
                "admin@example.com"
        );

        assertEquals("Updated Name", result.getName());
        assertEquals(64, result.getMaxPlayers());
        verify(tournamentRepository).save(any(Tournament.class));
    }

    @Test
    void should_throw_when_tournament_not_found_on_general_info_update() {
        UUID tournamentId = UUID.randomUUID();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> tournamentService.updateGeneralInfo(
                        tournamentId, "New Name", null, null, null, null, null, null,
                        null, null, null, null, "admin@example.com"
                )
        );
    }

    @Test
    void should_throw_when_tournament_not_in_editable_state_on_general_info_update() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.IN_PROGRESS)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> tournamentService.updateGeneralInfo(
                        tournamentId, "New Name", null, null, null, null, null, null,
                        null, null, null, null, "admin@example.com"
                )
        );

        assertTrue(exception.getMessage().contains("Solo se puede editar la información general"));
    }

    @Test
    void should_allow_general_info_update_in_open_status() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.OPEN)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();
        Tournament savedTournament = tournament.toBuilder().name("Updated").build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);

        Tournament result = tournamentService.updateGeneralInfo(
                tournamentId, "Updated", null, null, null, null, null, null,
                null, null, null, null, "admin@example.com"
        );

        assertEquals("Updated", result.getName());
    }

    @Test
    void should_throw_when_name_is_empty_on_general_info_update() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> tournamentService.updateGeneralInfo(
                        tournamentId, "  ", null, null, null, null, null, null,
                        null, null, null, null, "admin@example.com"
                )
        );

        assertEquals("El nombre del torneo no puede estar vacío.", exception.getMessage());
    }

    @Test
    void should_throw_when_max_players_is_zero_or_negative() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> tournamentService.updateGeneralInfo(
                        tournamentId, null, null, null, null, null, 0, null,
                        null, null, null, null, "admin@example.com"
                )
        );

        assertEquals("El número máximo de jugadores debe ser mayor que cero.", exception.getMessage());
    }

    @Test
    void should_throw_when_max_players_is_negative() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));

        assertThrows(
                InvalidArgumentException.class,
                () -> tournamentService.updateGeneralInfo(
                        tournamentId, null, null, null, null, null, -5, null,
                        null, null, null, null, "admin@example.com"
                )
        );
    }

    @Test
    void should_throw_when_location_is_empty_on_general_info_update() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> tournamentService.updateGeneralInfo(
                        tournamentId, null, null, null, null, null, null, "  ",
                        null, null, null, null, "admin@example.com"
                )
        );

        assertEquals("La ubicación del torneo no puede estar vacía.", exception.getMessage());
    }

    @Test
    void should_trim_name_and_location_on_general_info_update() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tournament result = tournamentService.updateGeneralInfo(
                tournamentId, "  Trimmed Name  ", null, null, null, null, null, "  Trimmed Location  ",
                null, null, null, null, "admin@example.com"
        );

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());

        assertEquals("Trimmed Name", captor.getValue().getName());
        assertEquals("Trimmed Location", captor.getValue().getLocation());
    }

    @Test
    void should_update_all_general_info_fields() {
        UUID tournamentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Old Name")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Old Location")
                .state(TournamentStatus.DRAFT)
                .createdBy(Member.builder().id(creatorId).email("admin@example.com").build())
                .events(List.of())
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(Member.builder()
                .id(creatorId)
                .email("admin@example.com")
                .build()));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentPeriod newPlayPeriod = new TournamentPeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));
        TournamentPeriod newInscriptionPeriod = new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 20));

        tournamentService.updateGeneralInfo(
                tournamentId,
                "New Name",
                newPlayPeriod,
                LocalTime.of(10, 0),
                newInscriptionPeriod,
                Surface.HARD,
                64,
                "New Location",
                40.4168,
                -3.7038,
                "placeId123",
                "New Address",
                "admin@example.com"
        );

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());

        Tournament saved = captor.getValue();
        assertEquals("New Name", saved.getName());
        assertEquals(newPlayPeriod, saved.getPlayPeriod());
        assertEquals(LocalTime.of(10, 0), saved.getStartTime());
        assertEquals(newInscriptionPeriod, saved.getInscriptionPeriod());
        assertEquals(Surface.HARD, saved.getSurface());
        assertEquals(64, saved.getMaxPlayers());
        assertEquals("New Location", saved.getLocation());
        assertEquals(40.4168, saved.getLocationLatitude());
        assertEquals(-3.7038, saved.getLocationLongitude());
        assertEquals("placeId123", saved.getLocationPlaceId());
        assertEquals("New Address", saved.getLocationFormattedAddress());
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
