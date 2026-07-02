package com.tfm.tennis_platform.infrastructure.controller;

import com.tfm.tennis_platform.application.services.CourtService;
import com.tfm.tennis_platform.application.services.EventService;
import com.tfm.tennis_platform.application.services.InscriptionService;
import com.tfm.tennis_platform.application.services.MatchService;
import com.tfm.tennis_platform.application.services.ScheduleConfigService;
import com.tfm.tennis_platform.application.services.TournamentService;
import com.tfm.tennis_platform.domain.exceptions.ResourceNotFoundException;
import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.ScheduleConfig;
import com.tfm.tennis_platform.domain.models.TimeSlot;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.TournamentSummary;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.ScheduleTimeType;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.infrastructure.controller.dto.CourtRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.CourtResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResultRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.MatchScheduleRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ScheduleConfigRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.ScheduleConfigResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentResponse;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentStatusUpdateRequest;
import com.tfm.tennis_platform.infrastructure.controller.dto.TournamentSummaryResponse;
import com.tfm.tennis_platform.infrastructure.controller.mapper.MatchWebMapper;
import com.tfm.tennis_platform.infrastructure.controller.mapper.TournamentWebMapper;
import com.tfm.tennis_platform.infrastructure.pdf.TournamentPdfExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TournamentController")
class TournamentControllerTest {

    @Mock private TournamentService tournamentService;
    @Mock private MatchService matchService;
    @Mock private TournamentWebMapper tournamentWebMapper;
    @Mock private MatchWebMapper matchWebMapper;
    @Mock private EventService eventService;
    @Mock private InscriptionService inscriptionService;
    @Mock private CourtService courtService;
    @Mock private TournamentPdfExporter tournamentPdfExporter;
    @Mock private ScheduleConfigService scheduleConfigService;

    @InjectMocks
    private TournamentController controller;

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
                .state(TournamentStatus.OPEN)
                .build();
    }

    private TournamentResponse buildTournamentResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(), tournament.getName(),
                tournament.getPlayPeriod().startDate(), tournament.getPlayPeriod().endDate(),
                tournament.getStartTime(),
                tournament.getInscriptionPeriod().startDate(), tournament.getInscriptionPeriod().endDate(),
                tournament.getSurface(), tournament.getMaxPlayers(),
                tournament.getLocation(), null, null, null, null,
                tournament.getState(), null, null, false, null, null
        );
    }

    private Court buildCourt(UUID tournamentId) {
        return Court.builder()
                .id(UUID.randomUUID())
                .tournamentId(tournamentId)
                .name("Pista 1")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("should create tournament and return 201")
        void should_create_tournament() {
            TournamentRequest request = new TournamentRequest(
                    "Open de Primavera", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10),
                    null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20),
                    Surface.CLAY, 32, "Club Central", null, null, null, null, 3
            );
            Tournament domain = buildTournament();
            TournamentResponse responseDto = buildTournamentResponse(domain);

            when(tournamentWebMapper.toDomain(request)).thenReturn(domain);
            when(tournamentService.create(eq(domain), eq("admin@test.com"), eq(3))).thenReturn(domain);
            when(tournamentService.isProfessionalTournament(domain.getId())).thenReturn(false);
            when(tournamentWebMapper.toResponse(domain)).thenReturn(responseDto);

            ResponseEntity<TournamentResponse> response = controller.create(request, principal());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().formalName()).isEqualTo("Open de Primavera");
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAllTests {

        @Test
        @DisplayName("should return list of tournament summaries")
        void should_return_summaries() {
            TournamentSummary summary = new TournamentSummary(
                    UUID.randomUUID(), "Open 2026",
                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10),
                    LocalTime.of(9, 0),
                    LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20),
                    Surface.CLAY, 32, "Madrid", TournamentStatus.OPEN, false
            );
            when(tournamentService.findSummaries()).thenReturn(List.of(summary));

            ResponseEntity<List<TournamentSummaryResponse>> response = controller.getAll();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no tournaments")
        void should_return_empty_list() {
            when(tournamentService.findSummaries()).thenReturn(List.of());

            ResponseEntity<List<TournamentSummaryResponse>> response = controller.getAll();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("should return tournament when found")
        void should_return_tournament_when_found() {
            Tournament tournament = buildTournament();
            TournamentResponse responseDto = buildTournamentResponse(tournament);

            when(tournamentService.findById(tournament.getId())).thenReturn(Optional.of(tournament));
            when(tournamentService.isProfessionalTournament(tournament.getId())).thenReturn(false);
            when(tournamentWebMapper.toResponse(tournament)).thenReturn(responseDto);

            ResponseEntity<TournamentResponse> response = controller.getById(tournament.getId());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void should_throw_when_not_found() {
            UUID id = UUID.randomUUID();
            when(tournamentService.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.getById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("should update tournament status")
        void should_update_status() {
            Tournament updated = buildTournament();
            TournamentResponse responseDto = buildTournamentResponse(updated);
            TournamentStatusUpdateRequest request = new TournamentStatusUpdateRequest(TournamentStatus.CLOSED);

            when(tournamentService.updateStatus(updated.getId(), TournamentStatus.CLOSED, "admin@test.com")).thenReturn(updated);
            when(tournamentService.isProfessionalTournament(updated.getId())).thenReturn(false);
            when(tournamentWebMapper.toResponse(updated)).thenReturn(responseDto);

            ResponseEntity<TournamentResponse> response = controller.updateStatus(updated.getId(), request, principal());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Nested
    @DisplayName("getCourts / createCourt / updateCourt / deleteCourt")
    class CourtTests {

        @Test
        @DisplayName("should return courts for tournament")
        void should_return_courts() {
            UUID tournamentId = UUID.randomUUID();
            Court court = buildCourt(tournamentId);
            when(courtService.findByTournamentId(tournamentId)).thenReturn(List.of(court));

            ResponseEntity<List<CourtResponse>> response = controller.getCourts(tournamentId);

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("should create court and return 201")
        void should_create_court() {
            UUID tournamentId = UUID.randomUUID();
            Court court = buildCourt(tournamentId);
            CourtRequest request = new CourtRequest("Pista 1");
            when(courtService.create(tournamentId, "Pista 1", "admin@test.com")).thenReturn(court);

            ResponseEntity<CourtResponse> response = controller.createCourt(tournamentId, request, principal());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should update court")
        void should_update_court() {
            UUID tournamentId = UUID.randomUUID();
            UUID courtId = UUID.randomUUID();
            Court court = Court.builder().id(courtId).tournamentId(tournamentId).name("Pista 2").active(true).build();
            CourtRequest request = new CourtRequest("Pista 2");
            when(courtService.update(tournamentId, courtId, "Pista 2", "admin@test.com")).thenReturn(court);

            ResponseEntity<CourtResponse> response = controller.updateCourt(tournamentId, courtId, request, principal());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }

        @Test
        @DisplayName("should delete court and return 204")
        void should_delete_court() {
            UUID tournamentId = UUID.randomUUID();
            UUID courtId = UUID.randomUUID();

            ResponseEntity<Void> response = controller.deleteCourt(tournamentId, courtId, principal());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(courtService).delete(tournamentId, courtId, "admin@test.com");
        }
    }

    @Nested
    @DisplayName("getScheduleConfig / saveScheduleConfig")
    class ScheduleConfigTests {

        @Test
        @DisplayName("should return default config when null")
        void should_return_default_config_when_null() {
            UUID tournamentId = UUID.randomUUID();
            when(scheduleConfigService.findByTournamentId(tournamentId)).thenReturn(null);

            ResponseEntity<ScheduleConfigResponse> response = controller.getScheduleConfig(tournamentId);

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody().matchDurationMinutes()).isEqualTo(60);
        }

        @Test
        @DisplayName("should return config when exists")
        void should_return_config_when_exists() {
            UUID tournamentId = UUID.randomUUID();
            ScheduleConfig config = ScheduleConfig.builder()
                    .id(UUID.randomUUID())
                    .tournamentId(tournamentId)
                    .timeSlots(List.of(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                    .matchDurationMinutes(90)
                    .build();
            when(scheduleConfigService.findByTournamentId(tournamentId)).thenReturn(config);

            ResponseEntity<ScheduleConfigResponse> response = controller.getScheduleConfig(tournamentId);

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody().matchDurationMinutes()).isEqualTo(90);
        }

        @Test
        @DisplayName("should save schedule config")
        void should_save_schedule_config() {
            UUID tournamentId = UUID.randomUUID();
            ScheduleConfigRequest.TimeSlotRequest slotReq = new ScheduleConfigRequest.TimeSlotRequest(
                    LocalTime.of(9, 0), LocalTime.of(10, 0));
            ScheduleConfigRequest request = new ScheduleConfigRequest(List.of(slotReq), 90);
            ScheduleConfig config = ScheduleConfig.builder()
                    .id(UUID.randomUUID())
                    .tournamentId(tournamentId)
                    .timeSlots(List.of(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                    .matchDurationMinutes(90)
                    .build();
            when(scheduleConfigService.save(eq(tournamentId), any(), eq(90), eq("admin@test.com"))).thenReturn(config);

            ResponseEntity<ScheduleConfigResponse> response = controller.saveScheduleConfig(tournamentId, request, principal());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }

        @Test
        @DisplayName("should save schedule config with null time slots")
        void should_save_with_null_time_slots() {
            UUID tournamentId = UUID.randomUUID();
            ScheduleConfigRequest request = new ScheduleConfigRequest(null, 60);
            ScheduleConfig config = ScheduleConfig.builder()
                    .id(UUID.randomUUID())
                    .tournamentId(tournamentId)
                    .timeSlots(List.of())
                    .matchDurationMinutes(60)
                    .build();
            when(scheduleConfigService.save(eq(tournamentId), any(), eq(60), eq("admin@test.com"))).thenReturn(config);

            ResponseEntity<ScheduleConfigResponse> response = controller.saveScheduleConfig(tournamentId, request, principal());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Nested
    @DisplayName("generateDraws")
    class GenerateDrawsTests {

        @Test
        @DisplayName("should generate draws for event")
        void should_generate_draws() {
            UUID tournamentId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            Tournament updated = buildTournament();
            TournamentResponse responseDto = buildTournamentResponse(updated);

            when(eventService.generateDrawsForEvent(tournamentId, eventId, "admin@test.com")).thenReturn(updated);
            when(tournamentService.isProfessionalTournament(updated.getId())).thenReturn(false);
            when(tournamentWebMapper.toResponse(updated)).thenReturn(responseDto);

            ResponseEntity<TournamentResponse> response = controller.generateDraws(tournamentId, eventId, principal());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Nested
    @DisplayName("recordMatchResult")
    class RecordMatchResultTests {

        @Test
        @DisplayName("should record match result")
        void should_record_result() {
            UUID tournamentId = UUID.randomUUID();
            UUID matchId = UUID.randomUUID();
            UUID winnerId = UUID.randomUUID();
            Match match = Match.builder().id(matchId).roundNumber(1).status(MatchStatus.COMPLETED).result("6-4 7-5").build();
            MatchResponse responseDto = new MatchResponse(
                    matchId, null, null, null, 1, null, null, null, null, null, "6-4 7-5", false, null, null, "COMPLETED");
            MatchResultRequest request = new MatchResultRequest(
                    winnerId, List.of(), "test notes", "40", "15", MatchStatus.COMPLETED);

            when(matchService.recordResult(tournamentId, matchId, winnerId, List.of(), "test notes", "40", "15", MatchStatus.COMPLETED, "admin@test.com")).thenReturn(match);
            when(matchWebMapper.toResponse(match)).thenReturn(responseDto);

            ResponseEntity<MatchResponse> response = controller.recordMatchResult(tournamentId, matchId, request, principal());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody().result()).isEqualTo("6-4 7-5");
        }
    }

    @Nested
    @DisplayName("scheduleMatch")
    class ScheduleMatchTests {

        @Test
        @DisplayName("should schedule match")
        void should_schedule_match() {
            UUID tournamentId = UUID.randomUUID();
            UUID matchId = UUID.randomUUID();
            UUID courtId = UUID.randomUUID();
            LocalDateTime scheduledAt = LocalDateTime.of(2026, 5, 5, 10, 0);
            Match match = Match.builder().id(matchId).roundNumber(1).status(MatchStatus.PENDING).build();
            MatchResponse responseDto = new MatchResponse(
                    matchId, null, null, null, 1, null, null, null, null, null, null, false, null, null, "PENDING");
            MatchScheduleRequest request = new MatchScheduleRequest(courtId, scheduledAt, ScheduleTimeType.EXACT, false);

            when(matchService.schedule(tournamentId, matchId, courtId, scheduledAt, ScheduleTimeType.EXACT, false, "admin@test.com")).thenReturn(match);
            when(matchWebMapper.toResponse(match)).thenReturn(responseDto);

            ResponseEntity<MatchResponse> response = controller.scheduleMatch(tournamentId, matchId, request, principal());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }

    @Nested
    @DisplayName("exportTournamentPdf")
    class ExportPdfTests {

        @Test
        @DisplayName("should export PDF with correct filename")
        void should_export_pdf() {
            UUID tournamentId = UUID.randomUUID();
            Tournament tournament = buildTournament();
            when(tournamentService.findById(tournamentId)).thenReturn(Optional.of(tournament));
            when(inscriptionService.findByTournament(tournamentId, null)).thenReturn(null);
            when(courtService.findByTournamentId(tournamentId)).thenReturn(List.of());
            when(matchService.findByTournamentId(tournamentId)).thenReturn(List.of());
            when(tournamentPdfExporter.exportTournamentData(eq(tournament), eq(null), any(), any())).thenReturn(new byte[]{1, 2, 3});

            ResponseEntity<byte[]> response = controller.exportTournamentPdf(tournamentId);

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isEqualTo(new byte[]{1, 2, 3});
            assertThat(response.getHeaders().getContentType().toString()).contains("application/pdf");
        }

        @Test
        @DisplayName("should throw when tournament not found")
        void should_throw_when_tournament_not_found_for_pdf() {
            UUID tournamentId = UUID.randomUUID();
            when(tournamentService.findById(tournamentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.exportTournamentPdf(tournamentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("sanitizeFilename")
    class SanitizeFilenameTests {

        @Test
        @DisplayName("should sanitize special characters")
        void should_sanitize_special_characters() {
            UUID tournamentId = UUID.randomUUID();
            Tournament tournament = Tournament.builder()
                    .id(tournamentId)
                    .name("Open de Primavera 2026!")
                    .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                    .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                    .surface(Surface.CLAY)
                    .maxPlayers(32)
                    .location("Madrid")
                    .state(TournamentStatus.OPEN)
                    .build();
            when(tournamentService.findById(tournamentId)).thenReturn(Optional.of(tournament));
            when(inscriptionService.findByTournament(tournamentId, null)).thenReturn(null);
            when(courtService.findByTournamentId(tournamentId)).thenReturn(List.of());
            when(matchService.findByTournamentId(tournamentId)).thenReturn(List.of());
            when(tournamentPdfExporter.exportTournamentData(any(), any(), any(), any())).thenReturn(new byte[]{});

            ResponseEntity<byte[]> response = controller.exportTournamentPdf(tournamentId);

            String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
            assertThat(contentDisposition).contains("open_de_primavera_2026.pdf");
        }
    }
}
