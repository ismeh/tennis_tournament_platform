package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.DuplicateResourceException;
import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

    @Mock
    private CourtRepository courtRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private CourtService courtService;

    private static final UUID TOURNAMENT_ID = UUID.randomUUID();
    private static final UUID COURT_ID = UUID.randomUUID();
    private static final String ADMIN_EMAIL = "admin@example.com";

    private Tournament buildTournament(TournamentStatus status) {
        Member admin = Member.builder().id(UUID.randomUUID()).email(ADMIN_EMAIL).role(UserRole.ORGANIZER).build();
        return Tournament.builder()
                .id(TOURNAMENT_ID)
                .name("Test Tournament")
                .playPeriod(new TournamentPeriod(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.now(), LocalDate.now().plusDays(5)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Madrid")
                .state(status)
                .createdBy(admin)
                .build();
    }

    @Test
    void findByTournamentId_returns_courts() {
        Court court = Court.builder().id(COURT_ID).tournamentId(TOURNAMENT_ID).name("Pista 1").active(true).build();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.OPEN)));
        when(courtRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(List.of(court));

        List<Court> result = courtService.findByTournamentId(TOURNAMENT_ID);

        assertEquals(1, result.size());
        assertEquals("Pista 1", result.get(0).getName());
    }

    @Test
    void findByTournamentId_throws_when_tournament_not_found() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courtService.findByTournamentId(TOURNAMENT_ID));
    }

    @Test
    void findByTournamentId_throws_when_tournamentId_is_null() {
        assertThrows(InvalidArgumentException.class, () -> courtService.findByTournamentId(null));
    }

    @Test
    void create_saves_new_court() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));
        when(courtRepository.existsByTournamentIdAndName(TOURNAMENT_ID, "Pista 1")).thenReturn(false);
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        Court result = courtService.create(TOURNAMENT_ID, "Pista 1", ADMIN_EMAIL);

        assertEquals("Pista 1", result.getName());
        assertTrue(result.isActive());
        verify(courtRepository).save(any(Court.class));
    }

    @Test
    void create_throws_when_name_is_blank() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));

        assertThrows(InvalidArgumentException.class, () -> courtService.create(TOURNAMENT_ID, "  ", ADMIN_EMAIL));
        assertThrows(InvalidArgumentException.class, () -> courtService.create(TOURNAMENT_ID, null, ADMIN_EMAIL));
    }

    @Test
    void create_throws_when_name_already_exists() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));
        when(courtRepository.existsByTournamentIdAndName(TOURNAMENT_ID, "Pista 1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> courtService.create(TOURNAMENT_ID, "Pista 1", ADMIN_EMAIL));
    }

    @Test
    void create_throws_when_tournament_is_closed() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.CLOSED)));

        assertThrows(InvalidArgumentException.class, () -> courtService.create(TOURNAMENT_ID, "Pista 1", ADMIN_EMAIL));
    }

    @Test
    void create_throws_when_tournament_is_in_progress() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.IN_PROGRESS)));

        assertThrows(InvalidArgumentException.class, () -> courtService.create(TOURNAMENT_ID, "Pista 1", ADMIN_EMAIL));
    }

    @Test
    void create_throws_when_tournament_is_completed() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.COMPLETED)));

        assertThrows(InvalidArgumentException.class, () -> courtService.create(TOURNAMENT_ID, "Pista 1", ADMIN_EMAIL));
    }

    @Test
    void create_throws_when_tournament_is_cancelled() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.CANCELLED)));

        assertThrows(InvalidArgumentException.class, () -> courtService.create(TOURNAMENT_ID, "Pista 1", ADMIN_EMAIL));
    }

    @Test
    void update_modifies_court_name() {
        Court existingCourt = Court.builder().id(COURT_ID).tournamentId(TOURNAMENT_ID).name("Old Name").active(true).build();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));
        when(courtRepository.findByIdAndTournamentId(COURT_ID, TOURNAMENT_ID)).thenReturn(Optional.of(existingCourt));
        when(courtRepository.existsByTournamentIdAndName(TOURNAMENT_ID, "New Name")).thenReturn(false);
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        Court result = courtService.update(TOURNAMENT_ID, COURT_ID, "New Name", ADMIN_EMAIL);

        assertEquals("New Name", result.getName());
    }

    @Test
    void update_throws_when_courtId_is_null() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));

        assertThrows(InvalidArgumentException.class, () -> courtService.update(TOURNAMENT_ID, null, "Name", ADMIN_EMAIL));
    }

    @Test
    void update_throws_when_court_not_found() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));
        when(courtRepository.findByIdAndTournamentId(COURT_ID, TOURNAMENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courtService.update(TOURNAMENT_ID, COURT_ID, "Name", ADMIN_EMAIL));
    }

    @Test
    void update_throws_when_name_conflict() {
        Court existingCourt = Court.builder().id(COURT_ID).tournamentId(TOURNAMENT_ID).name("Old").active(true).build();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));
        when(courtRepository.findByIdAndTournamentId(COURT_ID, TOURNAMENT_ID)).thenReturn(Optional.of(existingCourt));
        when(courtRepository.existsByTournamentIdAndName(TOURNAMENT_ID, "Taken")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> courtService.update(TOURNAMENT_ID, COURT_ID, "Taken", ADMIN_EMAIL));
    }

    @Test
    void update_allows_same_name() {
        Court existingCourt = Court.builder().id(COURT_ID).tournamentId(TOURNAMENT_ID).name("Same").active(true).build();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));
        when(courtRepository.findByIdAndTournamentId(COURT_ID, TOURNAMENT_ID)).thenReturn(Optional.of(existingCourt));
        when(courtRepository.save(any(Court.class))).thenAnswer(inv -> inv.getArgument(0));

        Court result = courtService.update(TOURNAMENT_ID, COURT_ID, "same", ADMIN_EMAIL);

        assertEquals("same", result.getName());
    }

    @Test
    void delete_removes_court() {
        Court existingCourt = Court.builder().id(COURT_ID).tournamentId(TOURNAMENT_ID).name("ToDelete").active(true).build();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));
        when(courtRepository.findByIdAndTournamentId(COURT_ID, TOURNAMENT_ID)).thenReturn(Optional.of(existingCourt));

        courtService.delete(TOURNAMENT_ID, COURT_ID, ADMIN_EMAIL);

        verify(courtRepository).deleteById(COURT_ID);
    }

    @Test
    void delete_throws_when_courtId_is_null() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));

        assertThrows(InvalidArgumentException.class, () -> courtService.delete(TOURNAMENT_ID, null, ADMIN_EMAIL));
    }

    @Test
    void delete_throws_when_court_not_found() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.DRAFT)));
        when(courtRepository.findByIdAndTournamentId(COURT_ID, TOURNAMENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courtService.delete(TOURNAMENT_ID, COURT_ID, ADMIN_EMAIL));
    }

    @Test
    void delete_throws_when_tournament_not_MODIFIABLE() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament(TournamentStatus.IN_PROGRESS)));

        assertThrows(InvalidArgumentException.class, () -> courtService.delete(TOURNAMENT_ID, COURT_ID, ADMIN_EMAIL));
    }
}
