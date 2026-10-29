package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentUmpire;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.UmpireSearchResult;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.MemberRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentUmpireRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentUmpireServiceTest {

    @Mock private TournamentUmpireRepository tournamentUmpireRepository;
    @Mock private TournamentRepository tournamentRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private TournamentService tournamentService;

    @InjectMocks
    private TournamentUmpireService tournamentUmpireService;

    private Tournament buildTournament(UUID id, UUID createdBy) {
        Member admin = Member.builder()
                .id(createdBy)
                .email("admin@test.com")
                .role(UserRole.ORGANIZER)
                .build();
        return Tournament.builder()
                .id(id)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(TournamentStatus.DRAFT)
                .createdBy(admin)
                .build();
    }

    @Test
    void should_add_umpire_to_tournament() {
        UUID tournamentId = UUID.randomUUID();
        UUID umpireId = UUID.randomUUID();
        Tournament tournament = buildTournament(tournamentId, UUID.randomUUID());
        Member umpire = Member.builder().id(umpireId).email("umpire@test.com").role(UserRole.UMPIRE).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findById(umpireId)).thenReturn(Optional.of(umpire));
        when(tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId)).thenReturn(false);
        when(tournamentUmpireRepository.save(any(TournamentUmpire.class))).thenAnswer(inv -> inv.getArgument(0));

        TournamentUmpire result = tournamentUmpireService.addUmpire(tournamentId, umpireId, "admin@test.com");

        assertNotNull(result);
        assertEquals(tournamentId, result.getTournamentId());
        assertEquals(umpireId, result.getUmpireId());
        verify(tournamentUmpireRepository).save(any(TournamentUmpire.class));
    }

    @Test
    void should_throw_when_tournament_not_found_on_add() {
        UUID tournamentId = UUID.randomUUID();
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tournamentUmpireService.addUmpire(tournamentId, UUID.randomUUID(), "admin@test.com"));
    }

    @Test
    void should_throw_when_member_not_found_on_add() {
        UUID tournamentId = UUID.randomUUID();
        UUID umpireId = UUID.randomUUID();
        Tournament tournament = buildTournament(tournamentId, UUID.randomUUID());

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findById(umpireId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> tournamentUmpireService.addUmpire(tournamentId, umpireId, "admin@test.com"));
    }

    @Test
    void should_throw_when_member_is_not_umpire_role() {
        UUID tournamentId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Tournament tournament = buildTournament(tournamentId, UUID.randomUUID());
        Member player = Member.builder().id(memberId).email("player@test.com").role(UserRole.PLAYER).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(player));

        InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
                () -> tournamentUmpireService.addUmpire(tournamentId, memberId, "admin@test.com"));
        assertTrue(ex.getMessage().contains("rol válido para ser árbitro"));
    }

    @Test
    void should_add_organizer_as_umpire_to_tournament() {
        UUID tournamentId = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        Tournament tournament = buildTournament(tournamentId, UUID.randomUUID());
        Member organizer = Member.builder().id(organizerId).email("organizer@test.com").role(UserRole.ORGANIZER).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findById(organizerId)).thenReturn(Optional.of(organizer));
        when(tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, organizerId)).thenReturn(false);
        when(tournamentUmpireRepository.save(any(TournamentUmpire.class))).thenAnswer(inv -> inv.getArgument(0));

        TournamentUmpire result = tournamentUmpireService.addUmpire(tournamentId, organizerId, "admin@test.com");

        assertNotNull(result);
        assertEquals(tournamentId, result.getTournamentId());
        assertEquals(organizerId, result.getUmpireId());
        verify(tournamentUmpireRepository).save(any(TournamentUmpire.class));
    }

    @Test
    void should_throw_when_umpire_already_assigned() {
        UUID tournamentId = UUID.randomUUID();
        UUID umpireId = UUID.randomUUID();
        Tournament tournament = buildTournament(tournamentId, UUID.randomUUID());
        Member umpire = Member.builder().id(umpireId).email("umpire@test.com").role(UserRole.UMPIRE).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(memberRepository.findById(umpireId)).thenReturn(Optional.of(umpire));
        when(tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId)).thenReturn(true);

        InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
                () -> tournamentUmpireService.addUmpire(tournamentId, umpireId, "admin@test.com"));
        assertTrue(ex.getMessage().contains("ya está asignado"));
    }

    @Test
    void should_remove_umpire_from_tournament() {
        UUID tournamentId = UUID.randomUUID();
        UUID umpireId = UUID.randomUUID();
        Tournament tournament = buildTournament(tournamentId, UUID.randomUUID());

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId)).thenReturn(true);

        tournamentUmpireService.removeUmpire(tournamentId, umpireId, "admin@test.com");

        verify(tournamentUmpireRepository).deleteByTournamentIdAndUmpireId(tournamentId, umpireId);
    }

    @Test
    void should_throw_when_removing_nonexistent_umpire() {
        UUID tournamentId = UUID.randomUUID();
        UUID umpireId = UUID.randomUUID();
        Tournament tournament = buildTournament(tournamentId, UUID.randomUUID());

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, umpireId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> tournamentUmpireService.removeUmpire(tournamentId, umpireId, "admin@test.com"));
    }

    @Test
    void should_find_by_tournament_id() {
        UUID tournamentId = UUID.randomUUID();
        TournamentUmpire umpire = TournamentUmpire.builder()
                .id(UUID.randomUUID())
                .tournamentId(tournamentId)
                .umpireId(UUID.randomUUID())
                .assignedAt(LocalDateTime.now())
                .build();

        when(tournamentUmpireRepository.findByTournamentId(tournamentId)).thenReturn(List.of(umpire));

        List<TournamentUmpire> result = tournamentUmpireService.findByTournamentId(tournamentId);

        assertEquals(1, result.size());
        assertEquals(tournamentId, result.getFirst().getTournamentId());
    }

    @Test
    void should_check_if_umpire_is_assigned() {
        UUID tournamentId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(tournamentUmpireRepository.existsByTournamentIdAndUmpireId(tournamentId, memberId)).thenReturn(true);

        assertTrue(tournamentUmpireService.isUmpireAssignedToTournament(tournamentId, memberId));
    }

    @Test
    void should_search_umpires() {
        String query = "test";
        UmpireSearchResult result = UmpireSearchResult.builder()
                .id(UUID.randomUUID())
                .email("umpire@test.com")
                .firstName("Test")
                .lastName("Umpire")
                .build();

        when(memberRepository.searchUmpiresWithPersonData(query)).thenReturn(List.of(result));

        List<UmpireSearchResult> umpires = tournamentUmpireService.searchUmpires(query);

        assertEquals(1, umpires.size());
        assertEquals("umpire@test.com", umpires.getFirst().getEmail());
    }

    @Test
    void should_search_by_multiple_roles() {
        String query = "test";
        List<UserRole> roles = List.of(UserRole.UMPIRE, UserRole.ORGANIZER);
        UmpireSearchResult umpireResult = UmpireSearchResult.builder()
                .id(UUID.randomUUID())
                .email("umpire@test.com")
                .firstName("Test")
                .lastName("Umpire")
                .build();
        UmpireSearchResult organizerResult = UmpireSearchResult.builder()
                .id(UUID.randomUUID())
                .email("organizer@test.com")
                .firstName("Test")
                .lastName("Organizer")
                .build();

        when(memberRepository.searchByRolesWithPersonData(roles, query)).thenReturn(List.of(umpireResult, organizerResult));

        List<UmpireSearchResult> results = tournamentUmpireService.searchByRoles(roles, query);

        assertEquals(2, results.size());
    }
}
