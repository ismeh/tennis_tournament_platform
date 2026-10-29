package com.tfm.tennis_platform.application.services;

import com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Member;
import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.models.TimeSlot;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.enums.UserRole;
import com.tfm.tennis_platform.domain.port.out.ScheduleConfigRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleConfigServiceTest {

    @Mock
    private ScheduleConfigRepository scheduleConfigRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private ScheduleConfigService scheduleConfigService;

    private static final UUID TOURNAMENT_ID = UUID.randomUUID();
    private static final String ADMIN_EMAIL = "admin@example.com";

    private Tournament buildTournament() {
        Member admin = Member.builder().id(UUID.randomUUID()).email(ADMIN_EMAIL).role(UserRole.ORGANIZER).build();
        return Tournament.builder()
                .id(TOURNAMENT_ID)
                .name("Test Tournament")
                .playPeriod(new TournamentPeriod(LocalDate.now().plusDays(10), LocalDate.now().plusDays(15)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.now(), LocalDate.now().plusDays(5)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Madrid")
                .state(TournamentStatus.DRAFT)
                .createdBy(admin)
                .build();
    }

    @Test
    void findByTournamentId_returns_config() {
        ScheduleConfig config = ScheduleConfig.builder()
                .id(UUID.randomUUID())
                .tournamentId(TOURNAMENT_ID)
                .matchDurationMinutes(60)
                .timeSlots(List.of(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .build();
        when(scheduleConfigRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.of(config));

        ScheduleConfig result = scheduleConfigService.findByTournamentId(TOURNAMENT_ID);

        assertNotNull(result);
        assertEquals(60, result.getMatchDurationMinutes());
    }

    @Test
    void findByTournamentId_returns_null_when_not_found() {
        when(scheduleConfigRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.empty());

        ScheduleConfig result = scheduleConfigService.findByTournamentId(TOURNAMENT_ID);

        assertNull(result);
    }

    @Test
    void save_creates_new_config_when_none_exists() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));
        when(scheduleConfigRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.empty());
        when(scheduleConfigRepository.save(any(ScheduleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        List<TimeSlot> slots = List.of(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0)));

        ScheduleConfig result = scheduleConfigService.save(TOURNAMENT_ID, slots, 60, ADMIN_EMAIL);

        assertNotNull(result.getId());
        assertEquals(TOURNAMENT_ID, result.getTournamentId());
        assertEquals(60, result.getMatchDurationMinutes());
        assertEquals(1, result.getTimeSlots().size());
    }

    @Test
    void save_updates_existing_config() {
        UUID existingId = UUID.randomUUID();
        ScheduleConfig existing = ScheduleConfig.builder()
                .id(existingId)
                .tournamentId(TOURNAMENT_ID)
                .matchDurationMinutes(45)
                .timeSlots(List.of())
                .build();

        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));
        when(scheduleConfigRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.of(existing));
        when(scheduleConfigRepository.save(any(ScheduleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        List<TimeSlot> slots = List.of(new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0)));

        ScheduleConfig result = scheduleConfigService.save(TOURNAMENT_ID, slots, 90, ADMIN_EMAIL);

        assertEquals(existingId, result.getId());
        assertEquals(90, result.getMatchDurationMinutes());
    }

    @Test
    void save_throws_when_duration_is_zero() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));

        assertThrows(InvalidArgumentException.class, () ->
                scheduleConfigService.save(TOURNAMENT_ID, List.of(), 0, ADMIN_EMAIL));
    }

    @Test
    void save_throws_when_duration_exceeds_480() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));

        assertThrows(InvalidArgumentException.class, () ->
                scheduleConfigService.save(TOURNAMENT_ID, List.of(), 481, ADMIN_EMAIL));
    }

    @Test
    void save_throws_when_duration_is_negative() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));

        assertThrows(InvalidArgumentException.class, () ->
                scheduleConfigService.save(TOURNAMENT_ID, List.of(), -1, ADMIN_EMAIL));
    }

    @Test
    void save_allows_non_overlapping_adjacent_slots() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));
        when(scheduleConfigRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.empty());
        when(scheduleConfigRepository.save(any(ScheduleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        List<TimeSlot> slots = List.of(
                new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new TimeSlot(LocalTime.of(10, 1), LocalTime.of(11, 0))
        );

        ScheduleConfig result = scheduleConfigService.save(TOURNAMENT_ID, slots, 60, ADMIN_EMAIL);

        assertEquals(2, result.getTimeSlots().size());
    }

    @Test
    void save_throws_when_slots_overlap() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));

        List<TimeSlot> slots = List.of(
                new TimeSlot(LocalTime.of(9, 0), LocalTime.of(11, 0)),
                new TimeSlot(LocalTime.of(10, 0), LocalTime.of(12, 0))
        );

        assertThrows(InvalidArgumentException.class, () ->
                scheduleConfigService.save(TOURNAMENT_ID, slots, 60, ADMIN_EMAIL));
    }

    @Test
    void save_throws_when_slots_touch_at_boundary() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));

        List<TimeSlot> slots = List.of(
                new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0))
        );

        assertThrows(InvalidArgumentException.class, () ->
                scheduleConfigService.save(TOURNAMENT_ID, slots, 60, ADMIN_EMAIL));
    }

    @Test
    void save_sorts_slots_by_start_time() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));
        when(scheduleConfigRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.empty());
        when(scheduleConfigRepository.save(any(ScheduleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        List<TimeSlot> slots = List.of(
                new TimeSlot(LocalTime.of(14, 0), LocalTime.of(15, 0)),
                new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0))
        );

        ScheduleConfig result = scheduleConfigService.save(TOURNAMENT_ID, slots, 60, ADMIN_EMAIL);

        assertEquals(LocalTime.of(9, 0), result.getTimeSlots().get(0).startTime());
        assertEquals(LocalTime.of(14, 0), result.getTimeSlots().get(1).startTime());
    }

    @Test
    void save_allows_null_timeSlots() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(buildTournament()));
        when(scheduleConfigRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.empty());
        when(scheduleConfigRepository.save(any(ScheduleConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduleConfig result = scheduleConfigService.save(TOURNAMENT_ID, null, 60, ADMIN_EMAIL);

        assertNotNull(result.getTimeSlots());
        assertTrue(result.getTimeSlots().isEmpty());
    }

    @Test
    void save_throws_when_tournament_not_found() {
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                scheduleConfigService.save(TOURNAMENT_ID, List.of(), 60, ADMIN_EMAIL));
    }
}
