package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.services.MatchService;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.port.out.CourtRepository;
import com.tfm.tennis_platform.domain.port.out.MatchRepository;
import com.tfm.tennis_platform.domain.port.out.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private CourtRepository courtRepository;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(matchRepository, tournamentRepository, courtRepository);
    }

    @Test
    void should_record_result_and_propagate_winner_to_next_match() {
        UUID tournamentId = UUID.randomUUID();
        UUID currentMatchId = UUID.randomUUID();
        UUID nextMatchId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        UUID loserId = UUID.randomUUID();

        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .build();

        Match nextMatch = Match.builder()
                .id(nextMatchId)
                .build();

        Match currentMatch = Match.builder()
                .id(currentMatchId)
                .firstInscription(Inscription.builder().id(winnerId).build())
                .secondInscription(Inscription.builder().id(loserId).build())
                .nextMatch(nextMatch)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(currentMatch, nextMatch));
        when(matchRepository.findById(nextMatchId.toString())).thenReturn(Optional.of(nextMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Match updatedCurrentMatch = matchService.recordResult(tournamentId, currentMatchId, winnerId, "6-4 6-3");

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
    }

    @Test
    void should_record_result_and_propagate_loser_to_consolation_match() {
        UUID tournamentId = UUID.randomUUID();
        UUID currentMatchId = UUID.randomUUID();
        UUID consolationMatchId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        UUID loserId = UUID.randomUUID();

        Tournament tournament = tournament(tournamentId);
        Match consolationMatch = Match.builder()
                .id(consolationMatchId)
                .build();
        Match currentMatch = Match.builder()
                .id(currentMatchId)
                .firstInscription(Inscription.builder().id(winnerId).build())
                .secondInscription(Inscription.builder().id(loserId).build())
                .loserNextMatch(consolationMatch)
                .build();

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(currentMatch, consolationMatch));
        when(matchRepository.findById(consolationMatchId.toString())).thenReturn(Optional.of(consolationMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Match updatedCurrentMatch = matchService.recordResult(tournamentId, currentMatchId, winnerId, "6-4 6-3");

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

        Tournament tournament = tournament(tournamentId);
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

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(currentMatch, nextMatch));
        when(matchRepository.findById(nextMatchId.toString())).thenReturn(Optional.of(nextMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Match updatedCurrentMatch = matchService.recordResult(tournamentId, currentMatchId, newWinnerId, "7-5 6-4");

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

        Tournament tournament = tournament(tournamentId);
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

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(currentMatch, nextMatch));
        when(matchRepository.findById(nextMatchId.toString())).thenReturn(Optional.of(nextMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        matchService.recordResult(tournamentId, currentMatchId, newWinnerId, "7-5 6-4");

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
        Tournament tournament = tournament(tournamentId);
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

        Match scheduledMatch = matchService.schedule(tournamentId, matchId, courtId, scheduledAt, ScheduleTimeType.NOT_BEFORE);

        assertEquals(scheduledAt, scheduledMatch.getScheduledAt());
        assertEquals(ScheduleTimeType.NOT_BEFORE, scheduledMatch.getScheduleTimeType());
        assertEquals(courtId, scheduledMatch.getCourtId());
        assertEquals("Pista 1", scheduledMatch.getCourt());
    }

    @Test
    void should_reject_schedule_when_court_is_already_busy_at_same_time() {
        UUID tournamentId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID otherMatchId = UUID.randomUUID();
        UUID courtId = UUID.randomUUID();
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 5, 2, 10, 0);
        Tournament tournament = tournament(tournamentId);
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
                () -> matchService.schedule(tournamentId, matchId, courtId, scheduledAt, ScheduleTimeType.EXACT)
        );
    }

    private Tournament tournament(UUID tournamentId) {
        return Tournament.builder()
                .id(tournamentId)
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .startTime(LocalTime.of(9, 0))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .build();
    }
}
