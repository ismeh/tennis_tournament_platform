package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.services.MatchService;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.port.out.ScheduleConfigRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentUpdatePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private ScheduleConfigRepository scheduleConfigRepository;

    @Mock
    private TournamentUpdatePublisher tournamentUpdatePublisher;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TournamentService tournamentService;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Match> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(matchRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        matchService = new MatchService(matchRepository, tournamentRepository, courtRepository, scheduleConfigRepository, tournamentUpdatePublisher, transactionTemplate, tournamentService);
    }

    @Test
    void should_record_result_and_propagate_winner_to_next_match() {
        UUID tournamentId = UUID.randomUUID();
        UUID currentMatchId = UUID.randomUUID();
        UUID nextMatchId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        UUID loserId = UUID.randomUUID();

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);

        Match nextMatch = Match.builder()
                .id(nextMatchId)
                .build();

        Match currentMatch = Match.builder()
                .id(currentMatchId)
                .firstInscription(Inscription.builder().id(winnerId).build())
                .secondInscription(Inscription.builder().id(loserId).build())
                .nextMatch(nextMatch)
                .build();

        when(matchRepository.findByIdAndTournamentId(currentMatchId, tournamentId)).thenReturn(Optional.of(currentMatch));
        when(matchRepository.findById(nextMatchId.toString())).thenReturn(Optional.of(nextMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        org.mockito.Mockito.doNothing().when(tournamentService).assertTournamentAdmin(tournamentId, "organizer@example.com");

        Match updatedCurrentMatch = matchService.recordResult(tournamentId, currentMatchId, winnerId, "6-4 6-3", MatchStatus.COMPLETED, "organizer@example.com");

        assertEquals(winnerId, updatedCurrentMatch.getWinnerId());
        assertEquals("6-4 6-3", updatedCurrentMatch.getResult());

        verify(matchRepository, times(2)).save(any(Match.class));
        verify(matchRepository).save(org.mockito.ArgumentMatchers.argThat(match -> {
            if (!nextMatchId.equals(match.getId())) {
                return false;
            }

            assertNotNull(match.getFirstInscriptionId());
            return winnerId.equals(match.getFirstInscriptionId());
        }));
        verify(tournamentUpdatePublisher).publish(org.mockito.ArgumentMatchers.argThat(event ->
                "MATCH_RESULT_UPDATED".equals(event.type().name())
                        && tournamentId.equals(event.tournamentId())
                        && currentMatchId.equals(event.matchId())
        ));
    }

    @Test
    void should_record_result_and_propagate_loser_to_consolation_match() {
        UUID tournamentId = UUID.randomUUID();
        UUID currentMatchId = UUID.randomUUID();
        UUID consolationMatchId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        UUID loserId = UUID.randomUUID();

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Match consolationMatch = Match.builder()
                .id(consolationMatchId)
                .build();
        Match currentMatch = Match.builder()
                .id(currentMatchId)
                .firstInscription(Inscription.builder().id(winnerId).build())
                .secondInscription(Inscription.builder().id(loserId).build())
                .loserNextMatch(consolationMatch)
                .build();

        when(matchRepository.findByIdAndTournamentId(currentMatchId, tournamentId)).thenReturn(Optional.of(currentMatch));
        when(matchRepository.findById(consolationMatchId.toString())).thenReturn(Optional.of(consolationMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        org.mockito.Mockito.doNothing().when(tournamentService).assertTournamentAdmin(tournamentId, "organizer@example.com");

        Match updatedCurrentMatch = matchService.recordResult(tournamentId, currentMatchId, winnerId, "6-4 6-3", MatchStatus.COMPLETED, "organizer@example.com");

        assertEquals(winnerId, updatedCurrentMatch.getWinnerId());
        verify(matchRepository, times(2)).save(any(Match.class));
        verify(matchRepository).save(org.mockito.ArgumentMatchers.argThat(match ->
                consolationMatchId.equals(match.getId())
                        && loserId.equals(match.getFirstInscriptionId())
        ));
    }

    @Test
    void should_replace_previous_winner_in_next_match_when_result_is_edited() {
        UUID tournamentId = UUID.randomUUID();
        UUID currentMatchId = UUID.randomUUID();
        UUID nextMatchId = UUID.randomUUID();
        UUID previousWinnerId = UUID.randomUUID();
        UUID newWinnerId = UUID.randomUUID();
        UUID otherAdvancedPlayerId = UUID.randomUUID();

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Match nextMatch = Match.builder()
                .id(nextMatchId)
                .firstInscription(Inscription.builder().id(previousWinnerId).build())
                .secondInscription(Inscription.builder().id(otherAdvancedPlayerId).build())
                .build();
        Match currentMatch = Match.builder()
                .id(currentMatchId)
                .firstInscription(Inscription.builder().id(previousWinnerId).build())
                .secondInscription(Inscription.builder().id(newWinnerId).build())
                .winner(Inscription.builder().id(previousWinnerId).build())
                .nextMatch(nextMatch)
                .build();

        when(matchRepository.findByIdAndTournamentId(currentMatchId, tournamentId)).thenReturn(Optional.of(currentMatch));
        when(matchRepository.findById(nextMatchId.toString())).thenReturn(Optional.of(nextMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        org.mockito.Mockito.doNothing().when(tournamentService).assertTournamentAdmin(tournamentId, "organizer@example.com");

        Match updatedCurrentMatch = matchService.recordResult(tournamentId, currentMatchId, newWinnerId, "7-5 6-4", MatchStatus.COMPLETED, "organizer@example.com");

        assertEquals(newWinnerId, updatedCurrentMatch.getWinnerId());
        verify(matchRepository, times(2)).save(any(Match.class));
        verify(matchRepository).save(org.mockito.ArgumentMatchers.argThat(match ->
                nextMatchId.equals(match.getId())
                        && newWinnerId.equals(match.getFirstInscriptionId())
                        && otherAdvancedPlayerId.equals(match.getSecondInscriptionId())
        ));
    }

    @Test
    void should_remove_duplicate_winner_when_replacing_previous_winner_in_next_match() {
        UUID tournamentId = UUID.randomUUID();
        UUID currentMatchId = UUID.randomUUID();
        UUID nextMatchId = UUID.randomUUID();
        UUID previousWinnerId = UUID.randomUUID();
        UUID newWinnerId = UUID.randomUUID();

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Match nextMatch = Match.builder()
                .id(nextMatchId)
                .firstInscription(Inscription.builder().id(previousWinnerId).build())
                .secondInscription(Inscription.builder().id(newWinnerId).build())
                .build();
        Match currentMatch = Match.builder()
                .id(currentMatchId)
                .firstInscription(Inscription.builder().id(previousWinnerId).build())
                .secondInscription(Inscription.builder().id(newWinnerId).build())
                .winner(Inscription.builder().id(previousWinnerId).build())
                .nextMatch(nextMatch)
                .build();

        when(matchRepository.findByIdAndTournamentId(currentMatchId, tournamentId)).thenReturn(Optional.of(currentMatch));
        when(matchRepository.findById(nextMatchId.toString())).thenReturn(Optional.of(nextMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        org.mockito.Mockito.doNothing().when(tournamentService).assertTournamentAdmin(tournamentId, "organizer@example.com");

        matchService.recordResult(tournamentId, currentMatchId, newWinnerId, "7-5 6-4", MatchStatus.COMPLETED, "organizer@example.com");

        verify(matchRepository).save(org.mockito.ArgumentMatchers.argThat(match -> {
            if (!nextMatchId.equals(match.getId())) {
                return false;
            }

            assertEquals(newWinnerId, match.getFirstInscriptionId());
            assertNull(match.getSecondInscriptionId());
            return true;
        }));
    }

    @Test
    void should_schedule_match_when_court_is_available() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 5, 2, 10, 0);
        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder()
                .id(courtId)
                .tournamentId(tournamentId)
                .name("Pista 1")
                .active(true)
                .build();
        Match match = Match.builder()
                .id(matchId)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Match scheduledMatch = matchService.schedule(tournamentId, matchId, courtId, scheduledAt, ScheduleTimeType.NOT_BEFORE, false, "organizer@example.com");

        assertEquals(scheduledAt, scheduledMatch.getScheduledAt());
        assertEquals(ScheduleTimeType.NOT_BEFORE, scheduledMatch.getScheduleTimeType());
        assertEquals(courtId, scheduledMatch.getCourtId());
        assertEquals("Pista 1", scheduledMatch.getCourt());
        verify(tournamentUpdatePublisher).publish(org.mockito.ArgumentMatchers.argThat(event ->
                "MATCH_SCHEDULE_UPDATED".equals(event.type().name())
                        && tournamentId.equals(event.tournamentId())
                        && matchId.equals(event.matchId())
        ));
    }

    @Test
    void should_reject_schedule_when_court_is_already_busy_at_same_time() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID otherMatchId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 5, 2, 10, 0);
        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder()
                .id(courtId)
                .tournamentId(tournamentId)
                .name("Pista 1")
                .active(true)
                .build();
        Match match = Match.builder()
                .id(matchId)
                .build();
        Match busyMatch = Match.builder()
                .id(otherMatchId)
                .courtId(courtId)
                .scheduledAt(scheduledAt)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(match, busyMatch));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException.class,
                () -> matchService.schedule(tournamentId, matchId, courtId, scheduledAt, ScheduleTimeType.EXACT, false, "organizer@example.com")
        );
    }

    @Test
    void should_cascade_replan_shift_subsequent_matches() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime timeA = LocalDateTime.of(2026, 5, 2, 10, 0);
        LocalDateTime timeB = LocalDateTime.of(2026, 5, 2, 11, 0);
        LocalDateTime newTimeA = LocalDateTime.of(2026, 5, 2, 12, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder().id(courtId).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(courtId).court("Pista 1").scheduledAt(timeA).scheduleTimeType(ScheduleTimeType.EXACT).build();
        Match matchB = Match.builder().id(matchBId).courtId(courtId).court("Pista 1").scheduledAt(timeB).scheduleTimeType(ScheduleTimeType.EXACT).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB));

        Match result = matchService.schedule(tournamentId, matchAId, courtId, newTimeA, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTimeA, result.getScheduledAt());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            if (matches.size() != 2) return false;
            Match updatedA = matches.stream().filter(m -> m.getId().equals(matchAId)).findFirst().orElse(null);
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            if (updatedA == null || updatedB == null) return false;
            return newTimeA.equals(updatedA.getScheduledAt())
                    && LocalDateTime.of(2026, 5, 2, 13, 0).equals(updatedB.getScheduledAt());
        }));
    }

    @Test
    void should_cascade_replan_with_court_change() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID court1Id = UUID.randomUUID();
        UUID court2Id = UUID.randomUUID();
        LocalDateTime timeA = LocalDateTime.of(2026, 5, 2, 10, 0);
        LocalDateTime timeB = LocalDateTime.of(2026, 5, 2, 11, 0);
        LocalDateTime newTimeA = LocalDateTime.of(2026, 5, 2, 12, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court1 = Court.builder().id(court1Id).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Court court2 = Court.builder().id(court2Id).tournamentId(tournamentId).name("Pista 2").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(court1Id).court("Pista 1").scheduledAt(timeA).scheduleTimeType(ScheduleTimeType.EXACT).build();
        Match matchB = Match.builder().id(matchBId).courtId(court2Id).court("Pista 2").scheduledAt(timeB).scheduleTimeType(ScheduleTimeType.EXACT).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(court2Id, tournamentId)).thenReturn(Optional.of(court2));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB));

        Match result = matchService.schedule(tournamentId, matchAId, court2Id, newTimeA, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTimeA, result.getScheduledAt());
        assertEquals(court2Id, result.getCourtId());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            if (matches.size() != 2) return false;
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            return updatedB != null && LocalDateTime.of(2026, 5, 2, 13, 0).equals(updatedB.getScheduledAt());
        }));
    }

    @Test
    void should_resolve_cascade_court_conflict_with_non_cascade_match() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID matchCId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime timeA = LocalDateTime.of(2026, 5, 2, 12, 0);
        LocalDateTime timeB = LocalDateTime.of(2026, 5, 2, 13, 0);
        LocalDateTime timeC = LocalDateTime.of(2026, 5, 2, 11, 0);
        LocalDateTime newTimeA = LocalDateTime.of(2026, 5, 2, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder().id(courtId).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(courtId).court("Pista 1").scheduledAt(timeA).build();
        Match matchB = Match.builder().id(matchBId).courtId(courtId).court("Pista 1").scheduledAt(timeB).build();
        Match matchC = Match.builder().id(matchCId).courtId(courtId).court("Pista 1").scheduledAt(timeC).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB, matchC));

        Match result = matchService.schedule(tournamentId, matchAId, courtId, newTimeA, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTimeA, result.getScheduledAt());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            if (updatedB == null) return false;
            return !updatedB.getScheduledAt().equals(timeC);
        }));
    }

    @Test
    void should_resolve_cascade_player_conflict_with_non_cascade_match() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID matchCId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        UUID court2Id = UUID.randomUUID();
        UUID court3Id = UUID.randomUUID();
        UUID playerX = UUID.randomUUID();
        UUID playerY = UUID.randomUUID();
        UUID playerZ = UUID.randomUUID();
        UUID playerW = UUID.randomUUID();
        UUID playerV = UUID.randomUUID();
        LocalDateTime timeA = LocalDateTime.of(2026, 5, 2, 12, 0);
        LocalDateTime timeB = LocalDateTime.of(2026, 5, 2, 13, 0);
        LocalDateTime timeC = LocalDateTime.of(2026, 5, 2, 11, 0);
        LocalDateTime newTimeA = LocalDateTime.of(2026, 5, 2, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder().id(courtId).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Court court2 = Court.builder().id(court2Id).tournamentId(tournamentId).name("Pista 2").active(true).build();
        Court court3 = Court.builder().id(court3Id).tournamentId(tournamentId).name("Pista 3").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(courtId).scheduledAt(timeA)
                .firstInscription(Inscription.builder().id(playerX).build())
                .secondInscription(Inscription.builder().id(playerY).build()).build();
        Match matchB = Match.builder().id(matchBId).courtId(court2Id).scheduledAt(timeB)
                .firstInscription(Inscription.builder().id(playerZ).build())
                .secondInscription(Inscription.builder().id(playerW).build()).build();
        Match matchC = Match.builder().id(matchCId).courtId(court3Id).scheduledAt(timeC)
                .firstInscription(Inscription.builder().id(playerZ).build())
                .secondInscription(Inscription.builder().id(playerV).build()).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB, matchC));

        Match result = matchService.schedule(tournamentId, matchAId, courtId, newTimeA, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTimeA, result.getScheduledAt());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            if (updatedB == null) return false;
            return !updatedB.getScheduledAt().equals(timeC);
        }));
    }

    @Test
    void should_reject_cascade_when_shifted_past_play_period() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime timeA = LocalDateTime.of(2026, 5, 8, 10, 0);
        LocalDateTime timeB = LocalDateTime.of(2026, 5, 9, 10, 0);
        LocalDateTime newTimeA = LocalDateTime.of(2026, 5, 10, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder().id(courtId).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(courtId).scheduledAt(timeA).build();
        Match matchB = Match.builder().id(matchBId).courtId(courtId).scheduledAt(timeB).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB));

        assertThrows(com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException.class,
                () -> matchService.schedule(tournamentId, matchAId, courtId, newTimeA, ScheduleTimeType.EXACT, true, "organizer@example.com"));
    }

    @Test
    void should_cascade_schedule_and_place_subsequent_matches_when_no_previous_time() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime newTime = LocalDateTime.of(2026, 5, 2, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder().id(courtId).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Match matchA = Match.builder().id(matchAId).roundNumber(1).bracketPosition(1).build();
        Match matchB = Match.builder().id(matchBId).roundNumber(1).bracketPosition(2).scheduledAt(LocalDateTime.of(2026, 5, 2, 11, 0)).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB));

        Match result = matchService.schedule(tournamentId, matchAId, courtId, newTime, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTime, result.getScheduledAt());
        assertEquals(courtId, result.getCourtId());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            if (matches.size() != 2) return false;
            Match updatedA = matches.stream().filter(m -> m.getId().equals(matchAId)).findFirst().orElse(null);
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            if (updatedA == null || updatedB == null) return false;
            return newTime.equals(updatedA.getScheduledAt())
                    && LocalDateTime.of(2026, 5, 2, 11, 0).equals(updatedB.getScheduledAt());
        }));
    }

    @Test
    void should_cascade_schedule_place_unscheduled_matches_sequentially() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID matchCId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime newTime = LocalDateTime.of(2026, 5, 2, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder().id(courtId).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Match matchA = Match.builder().id(matchAId).roundNumber(1).bracketPosition(1).build();
        Match matchB = Match.builder().id(matchBId).roundNumber(1).bracketPosition(2).build();
        Match matchC = Match.builder().id(matchCId).roundNumber(2).bracketPosition(1).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB, matchC));

        Match result = matchService.schedule(tournamentId, matchAId, courtId, newTime, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTime, result.getScheduledAt());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            if (matches.size() != 3) return false;
            Match updatedA = matches.stream().filter(m -> m.getId().equals(matchAId)).findFirst().orElse(null);
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            Match updatedC = matches.stream().filter(m -> m.getId().equals(matchCId)).findFirst().orElse(null);
            if (updatedA == null || updatedB == null || updatedC == null) return false;
            return newTime.equals(updatedA.getScheduledAt())
                    && LocalDateTime.of(2026, 5, 2, 11, 0).equals(updatedB.getScheduledAt())
                    && LocalDateTime.of(2026, 5, 2, 12, 0).equals(updatedC.getScheduledAt());
        }));
    }

    @Test
    void should_resolve_inter_cascade_court_conflict() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID court1Id = UUID.randomUUID();
        UUID court2Id = UUID.randomUUID();
        LocalDateTime sameTime = LocalDateTime.of(2026, 5, 2, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court1 = Court.builder().id(court1Id).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Court court2 = Court.builder().id(court2Id).tournamentId(tournamentId).name("Pista 2").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(court1Id).court("Pista 1").scheduledAt(sameTime).build();
        Match matchB = Match.builder().id(matchBId).courtId(court2Id).court("Pista 2").scheduledAt(sameTime).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(court2Id, tournamentId)).thenReturn(Optional.of(court2));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB));

        Match result = matchService.schedule(tournamentId, matchAId, court2Id, sameTime, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(sameTime, result.getScheduledAt());
        assertEquals(court2Id, result.getCourtId());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            if (updatedB == null) return false;
            return !updatedB.getScheduledAt().equals(sameTime) || !updatedB.getCourtId().equals(court2Id);
        }));
    }

    @Test
    void should_resolve_inter_cascade_player_conflict() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID court1Id = UUID.randomUUID();
        UUID court2Id = UUID.randomUUID();
        UUID court3Id = UUID.randomUUID();
        UUID playerX = UUID.randomUUID();
        UUID playerY = UUID.randomUUID();
        UUID playerZ = UUID.randomUUID();
        LocalDateTime sameTime = LocalDateTime.of(2026, 5, 2, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court1 = Court.builder().id(court1Id).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Court court2 = Court.builder().id(court2Id).tournamentId(tournamentId).name("Pista 2").active(true).build();
        Court court3 = Court.builder().id(court3Id).tournamentId(tournamentId).name("Pista 3").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(court1Id).scheduledAt(sameTime)
                .firstInscription(Inscription.builder().id(playerX).build())
                .secondInscription(Inscription.builder().id(playerY).build()).build();
        Match matchB = Match.builder().id(matchBId).courtId(court2Id).scheduledAt(sameTime)
                .firstInscription(Inscription.builder().id(playerX).build())
                .secondInscription(Inscription.builder().id(playerZ).build()).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(court3Id, tournamentId)).thenReturn(Optional.of(court3));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB));

        Match result = matchService.schedule(tournamentId, matchAId, court3Id, sameTime, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(sameTime, result.getScheduledAt());
        assertEquals(court3Id, result.getCourtId());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            if (updatedB == null) return false;
            return !updatedB.getScheduledAt().equals(sameTime) || !sharesPlayerWith(matchA, updatedB);
        }));
    }

    private boolean sharesPlayerWith(Match m1, Match m2) {
        if (m1.getFirstInscriptionId() == null && m1.getSecondInscriptionId() == null) return false;
        if (m2.getFirstInscriptionId() == null && m2.getSecondInscriptionId() == null) return false;
        boolean sameFirst = m1.getFirstInscriptionId() != null && m1.getFirstInscriptionId().equals(m2.getFirstInscriptionId());
        boolean sameSecond = m1.getSecondInscriptionId() != null && m1.getSecondInscriptionId().equals(m2.getSecondInscriptionId());
        boolean crossFirst = m1.getFirstInscriptionId() != null && m1.getFirstInscriptionId().equals(m2.getSecondInscriptionId());
        boolean crossSecond = m1.getSecondInscriptionId() != null && m1.getSecondInscriptionId().equals(m2.getFirstInscriptionId());
        return sameFirst || sameSecond || crossFirst || crossSecond;
    }

    @Test
    void should_cascade_replan_shift_backward() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime timeA = LocalDateTime.of(2026, 5, 2, 12, 0);
        LocalDateTime timeB = LocalDateTime.of(2026, 5, 2, 13, 0);
        LocalDateTime newTimeA = LocalDateTime.of(2026, 5, 2, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder().id(courtId).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(courtId).court("Pista 1").scheduledAt(timeA).build();
        Match matchB = Match.builder().id(matchBId).courtId(courtId).court("Pista 1").scheduledAt(timeB).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB));

        Match result = matchService.schedule(tournamentId, matchAId, courtId, newTimeA, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTimeA, result.getScheduledAt());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            if (matches.size() != 2) return false;
            Match updatedA = matches.stream().filter(m -> m.getId().equals(matchAId)).findFirst().orElse(null);
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            if (updatedA == null || updatedB == null) return false;
            return newTimeA.equals(updatedA.getScheduledAt())
                    && LocalDateTime.of(2026, 5, 2, 11, 0).equals(updatedB.getScheduledAt());
        }));
    }

    @Test
    void should_cascade_replan_assign_courts_to_shifted_matches() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID court1Id = UUID.randomUUID();
        UUID court2Id = UUID.randomUUID();
        LocalDateTime timeA = LocalDateTime.of(2026, 5, 2, 10, 0);
        LocalDateTime timeB = LocalDateTime.of(2026, 5, 2, 11, 0);
        LocalDateTime newTimeA = LocalDateTime.of(2026, 5, 2, 12, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court1 = Court.builder().id(court1Id).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Court court2 = Court.builder().id(court2Id).tournamentId(tournamentId).name("Pista 2").active(true).build();
        Match matchA = Match.builder().id(matchAId).courtId(court1Id).court("Pista 1").scheduledAt(timeA).build();
        Match matchB = Match.builder().id(matchBId).courtId(court1Id).court("Pista 1").scheduledAt(timeB).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(court1Id, tournamentId)).thenReturn(Optional.of(court1));
        when(courtRepository.findByTournamentId(tournamentId)).thenReturn(List.of(court1, court2));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB));

        Match result = matchService.schedule(tournamentId, matchAId, court1Id, newTimeA, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTimeA, result.getScheduledAt());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            if (matches.size() != 2) return false;
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            if (updatedB == null) return false;
            return updatedB.getCourtId() != null && updatedB.getCourt() != null;
        }));
    }

    @Test
    void should_cascade_schedule_assign_courts_to_unscheduled_matches() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchAId = UUID.randomUUID();
        UUID matchBId = UUID.randomUUID();
        UUID matchCId = UUID.randomUUID();
        UUID court1Id = UUID.randomUUID();
        UUID court2Id = UUID.randomUUID();
        LocalDateTime newTime = LocalDateTime.of(2026, 5, 2, 10, 0);

        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court1 = Court.builder().id(court1Id).tournamentId(tournamentId).name("Pista 1").active(true).build();
        Court court2 = Court.builder().id(court2Id).tournamentId(tournamentId).name("Pista 2").active(true).build();
        Match matchA = Match.builder().id(matchAId).roundNumber(1).bracketPosition(1).build();
        Match matchB = Match.builder().id(matchBId).roundNumber(1).bracketPosition(2).build();
        Match matchC = Match.builder().id(matchCId).roundNumber(2).bracketPosition(1).build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(court1Id, tournamentId)).thenReturn(Optional.of(court1));
        when(courtRepository.findByTournamentId(tournamentId)).thenReturn(List.of(court1, court2));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(matchA, matchB, matchC));

        Match result = matchService.schedule(tournamentId, matchAId, court1Id, newTime, ScheduleTimeType.EXACT, true, "organizer@example.com");

        assertEquals(newTime, result.getScheduledAt());
        verify(matchRepository).saveAll(org.mockito.ArgumentMatchers.argThat(matches -> {
            if (matches.size() != 3) return false;
            Match updatedA = matches.stream().filter(m -> m.getId().equals(matchAId)).findFirst().orElse(null);
            Match updatedB = matches.stream().filter(m -> m.getId().equals(matchBId)).findFirst().orElse(null);
            Match updatedC = matches.stream().filter(m -> m.getId().equals(matchCId)).findFirst().orElse(null);
            if (updatedA == null || updatedB == null || updatedC == null) return false;
            return updatedA.getCourtId() != null
                    && updatedB.getCourtId() != null
                    && updatedC.getCourtId() != null;
        }));
    }

    @Test
    void should_reject_schedule_when_court_is_already_busy_within_overlapping_interval() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID otherMatchId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 5, 2, 10, 30);
        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder()
                .id(courtId)
                .tournamentId(tournamentId)
                .name("Pista 1")
                .active(true)
                .build();
        Match match = Match.builder()
                .id(matchId)
                .build();
        Match busyMatch = Match.builder()
                .id(otherMatchId)
                .courtId(courtId)
                .scheduledAt(LocalDateTime.of(2026, 5, 2, 10, 0))
                .build();

        com.tfm.tennis_platform.domain.models.ScheduleConfig config = com.tfm.tennis_platform.domain.models.ScheduleConfig.builder()
                .tournamentId(tournamentId)
                .matchDurationMinutes(60)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(match, busyMatch));
        when(scheduleConfigRepository.findByTournamentId(tournamentId)).thenReturn(Optional.of(config));

        com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException.class,
                () -> matchService.schedule(tournamentId, matchId, courtId, scheduledAt, ScheduleTimeType.EXACT, false, "organizer@example.com")
        );
        assertEquals("La pista ya está ocupada en esa hora.", exception.getMessage());
    }

    @Test
    void should_accept_schedule_when_court_busy_is_outside_overlapping_interval() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID otherMatchId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 5, 2, 11, 0);
        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder()
                .id(courtId)
                .tournamentId(tournamentId)
                .name("Pista 1")
                .active(true)
                .build();
        Match match = Match.builder()
                .id(matchId)
                .build();
        Match busyMatch = Match.builder()
                .id(otherMatchId)
                .courtId(courtId)
                .scheduledAt(LocalDateTime.of(2026, 5, 2, 10, 0))
                .build();

        com.tfm.tennis_platform.domain.models.ScheduleConfig config = com.tfm.tennis_platform.domain.models.ScheduleConfig.builder()
                .tournamentId(tournamentId)
                .matchDurationMinutes(60)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(match, busyMatch));
        when(scheduleConfigRepository.findByTournamentId(tournamentId)).thenReturn(Optional.of(config));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Match scheduledMatch = matchService.schedule(tournamentId, matchId, courtId, scheduledAt, ScheduleTimeType.EXACT, false, "organizer@example.com");
        assertEquals(scheduledAt, scheduledMatch.getScheduledAt());
        assertEquals(courtId, scheduledMatch.getCourtId());
    }

    @Test
    void should_reject_schedule_when_custom_match_duration_overlaps() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID otherMatchId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 5, 2, 11, 0);
        Tournament tournament = tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.IN_PROGRESS);
        Court court = Court.builder()
                .id(courtId)
                .tournamentId(tournamentId)
                .name("Pista 1")
                .active(true)
                .build();
        Match match = Match.builder()
                .id(matchId)
                .build();
        Match busyMatch = Match.builder()
                .id(otherMatchId)
                .courtId(courtId)
                .scheduledAt(LocalDateTime.of(2026, 5, 2, 10, 0))
                .build();

        com.tfm.tennis_platform.domain.models.ScheduleConfig config = com.tfm.tennis_platform.domain.models.ScheduleConfig.builder()
                .tournamentId(tournamentId)
                .matchDurationMinutes(90)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(courtRepository.findByIdAndTournamentId(courtId, tournamentId)).thenReturn(Optional.of(court));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(match, busyMatch));
        when(scheduleConfigRepository.findByTournamentId(tournamentId)).thenReturn(Optional.of(config));

        com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                com.tfm.tennis_platform.domain.exceptions.InvalidArgumentException.class,
                () -> matchService.schedule(tournamentId, matchId, courtId, scheduledAt, ScheduleTimeType.EXACT, false, "organizer@example.com")
        );
        assertEquals("La pista ya está ocupada en esa hora.", exception.getMessage());
    }

    private Tournament tournament(UUID tournamentId) {
        return tournament(tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus.DRAFT);
    }

    private Tournament tournament(UUID tournamentId, com.tfm.tennis_platform.domain.models.enums.TournamentStatus status) {
        return Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .state(status)
                .build();
    }
}
