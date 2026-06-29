package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.MatchService;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import com.tfm.tennis_platform.infrastructure.controller.mapper.MatchWebMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchController")
class MatchControllerTest {

    @Mock private MatchService matchService;
    @Mock private MatchWebMapper matchMapper;

    @InjectMocks
    private MatchController controller;

    private Principal principal() {
        return () -> "admin@test.com";
    }

    private Tournament buildTournament() {
        return Tournament.builder()
                .id(UUID.randomUUID())
                .name("Open de Primavera")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Club Central")
                .build();
    }

    @Test
    @DisplayName("getByTournament returns matches for a tournament")
    void getByTournament() {
        UUID tournamentId = UUID.randomUUID();
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .tournament(buildTournament())
                .roundNumber(1)
                .status(MatchStatus.PENDING)
                .build();
        MatchResponse responseDto = new MatchResponse(
                match.getId(), null, null, null,
                1, null, null,
                null, null, null, null,
                false, null, null,
                "PENDING"
        );
        when(matchService.findByTournamentId(tournamentId)).thenReturn(List.of(match));
        when(matchMapper.toResponse(match)).thenReturn(responseDto);

        ResponseEntity<List<MatchResponse>> response = controller.getByTournament(tournamentId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("update modifies an existing match and returns it")
    void update() {
        UUID matchId = UUID.randomUUID();
        Tournament tournament = buildTournament();
        Match existing = Match.builder()
                .id(matchId)
                .tournament(tournament)
                .roundNumber(1)
                .status(MatchStatus.PENDING)
                .build();
        Match updated = Match.builder()
                .id(matchId)
                .tournament(tournament)
                .roundNumber(1)
                .result("7-5 6-4")
                .status(MatchStatus.COMPLETED)
                .build();
        MatchResponse requestDto = new MatchResponse(
                matchId, null, null, null,
                1, null, null,
                null, null, null, "7-5 6-4",
                false, null, null,
                "COMPLETED"
        );
        MatchResponse responseDto = new MatchResponse(
                matchId, null, null, null,
                1, null, null,
                null, null, null, "7-5 6-4",
                false, null, null,
                "COMPLETED"
        );

        when(matchMapper.toDomain(requestDto)).thenReturn(updated);
        when(matchService.findById(matchId.toString())).thenReturn(Optional.of(existing));
        when(matchService.update(any(Match.class), eq("admin@test.com"))).thenReturn(updated);
        when(matchMapper.toResponse(updated)).thenReturn(responseDto);

        ResponseEntity<MatchResponse> response = controller.update(matchId, requestDto, principal());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().result()).isEqualTo("7-5 6-4");
    }
}
