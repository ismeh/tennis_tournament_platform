package com.tfm.tennis_platform.application.service;

import com.tfm.tennis_platform.application.services.MatchService;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Tournament;
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

import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(matchRepository, tournamentRepository);
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
}