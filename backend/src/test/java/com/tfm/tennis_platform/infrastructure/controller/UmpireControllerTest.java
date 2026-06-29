package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.application.services.TournamentUmpireService;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.TournamentUmpire;
import com.tfm.tennis_platform.domain.models.UmpireSearchResult;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentUmpireRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UmpireController")
class UmpireControllerTest {

    @Mock private TournamentUmpireService tournamentUmpireService;
    @Mock private TournamentService tournamentService;

    @InjectMocks
    private UmpireController controller;

    private Principal principal() {
        return () -> "umpire@test.com";
    }

    @Test
    @DisplayName("getMyTournaments returns tournament summaries for the umpire")
    void getMyTournaments() {
        TournamentSummary summary = new TournamentSummary(
                UUID.randomUUID(), "Open de Primavera",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20),
                Surface.CLAY, 32, "Club Central",
                TournamentStatus.DRAFT, false
        );
        when(tournamentService.findSummariesByUmpire("umpire@test.com")).thenReturn(List.of(summary));

        ResponseEntity<?> response = controller.getMyTournaments(principal());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("searchUmpires returns matching umpires")
    void searchUmpires() {
        UmpireSearchResult result = UmpireSearchResult.builder()
                .id(UUID.randomUUID())
                .email("umpire@test.com")
                .firstName("Test")
                .lastName("Umpire")
                .build();
        when(tournamentUmpireService.searchUmpires("test")).thenReturn(List.of(result));

        ResponseEntity<?> response = controller.searchUmpires("test");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("getTournamentUmpires returns umpires assigned to a tournament")
    void getTournamentUmpires() {
        UUID tournamentId = UUID.randomUUID();
        TournamentUmpire umpire = TournamentUmpire.builder()
                .id(UUID.randomUUID())
                .tournamentId(tournamentId)
                .umpireId(UUID.randomUUID())
                .umpireEmail("umpire@test.com")
                .umpireFirstName("Test")
                .umpireLastName("Umpire")
                .assignedAt(LocalDateTime.now())
                .build();
        when(tournamentUmpireService.findByTournamentId(tournamentId)).thenReturn(List.of(umpire));

        ResponseEntity<?> response = controller.getTournamentUmpires(tournamentId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("addUmpire assigns an umpire and returns 201")
    void addUmpire() {
        UUID tournamentId = UUID.randomUUID();
        UUID umpireId = UUID.randomUUID();
        TournamentUmpire umpire = TournamentUmpire.builder()
                .id(UUID.randomUUID())
                .tournamentId(tournamentId)
                .umpireId(umpireId)
                .umpireEmail("umpire@test.com")
                .umpireFirstName("Test")
                .umpireLastName("Umpire")
                .assignedAt(LocalDateTime.now())
                .build();
        when(tournamentUmpireService.addUmpire(eq(tournamentId), eq(umpireId), any(String.class)))
                .thenReturn(umpire);

        ResponseEntity<?> response = controller.addUmpire(tournamentId, new TournamentUmpireRequest(umpireId), principal());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    @DisplayName("removeUmpire returns 204")
    void removeUmpire() {
        UUID tournamentId = UUID.randomUUID();
        UUID umpireId = UUID.randomUUID();
        doNothing().when(tournamentUmpireService).removeUmpire(tournamentId, umpireId, "umpire@test.com");

        ResponseEntity<Void> response = controller.removeUmpire(tournamentId, umpireId, principal());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(tournamentUmpireService).removeUmpire(tournamentId, umpireId, "umpire@test.com");
    }
}
