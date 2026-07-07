package com.tfm.tennis_platform.infrastructure.pdf;

import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionPlayerView;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TournamentPdfExporter")
class TournamentPdfExporterTest {

    private TournamentPdfExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new TournamentPdfExporter();
    }

    private Tournament buildTournament() {
        return Tournament.builder()
                .id(UUID.randomUUID())
                .name("Open de Primavera 2026")
                .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                .surface(Surface.CLAY)
                .maxPlayers(32)
                .location("Madrid")
                .locationFormattedAddress("Calle Mayor 1, Madrid")
                .startTime(LocalTime.of(9, 0))
                .state(TournamentStatus.OPEN)
                .build();
    }

    @Nested
    @DisplayName("exportTournamentData")
    class ExportTournamentDataTests {

        @Test
        @DisplayName("should generate PDF bytes for a complete tournament")
        void should_generate_pdf_for_complete_tournament() {
            Tournament tournament = buildTournament();
            UUID inscriptionId = UUID.randomUUID();
            TournamentInscriptionPlayerView player1 = new TournamentInscriptionPlayerView(
                    inscriptionId, UUID.randomUUID(), UUID.randomUUID(), 1,
                    "Senior", "Individual", "MALE",
                    UUID.randomUUID(), "EXISTING_PERSON", "LIC001",
                    "Carlos", "Garcia", "MALE", 100, null, null, null, null
            );
            TournamentInscriptionsView inscriptions = new TournamentInscriptionsView(
                    tournament.getId(), null, List.of(), List.of(), List.of(player1)
            );
            Court court1 = Court.builder()
                    .id(UUID.randomUUID())
                    .tournamentId(tournament.getId())
                    .name("Pista Central")
                    .active(true)
                    .build();
            List<Court> courts = List.of(court1);

            byte[] pdf = exporter.exportTournamentData(tournament, inscriptions, courts, List.of());

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("should generate PDF with null inscriptions")
        void should_generate_pdf_with_null_inscriptions() {
            Tournament tournament = buildTournament();

            byte[] pdf = exporter.exportTournamentData(tournament, null, List.of(), List.of());

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("should generate PDF with empty inscriptions")
        void should_generate_pdf_with_empty_inscriptions() {
            Tournament tournament = buildTournament();
            TournamentInscriptionsView emptyInscriptions = new TournamentInscriptionsView(
                    tournament.getId(), null, List.of(), List.of(), List.of()
            );

            byte[] pdf = exporter.exportTournamentData(tournament, emptyInscriptions, List.of(), List.of());

            assertThat(pdf).isNotNull();
            assertThat(pdf.length).isGreaterThan(0);
        }

        @Test
        @DisplayName("should generate PDF with null courts and matches")
        void should_generate_pdf_with_null_courts_and_matches() {
            Tournament tournament = buildTournament();

            byte[] pdf = exporter.exportTournamentData(tournament, null, null, null);

            assertThat(pdf).isNotNull();
        }

        @Test
        @DisplayName("should generate PDF with court active and inactive")
        void should_generate_pdf_with_mixed_court_status() {
            Tournament tournament = buildTournament();
            Court activeCourt = Court.builder().id(UUID.randomUUID()).tournamentId(tournament.getId()).name("Pista 1").active(true).build();
            Court inactiveCourt = Court.builder().id(UUID.randomUUID()).tournamentId(tournament.getId()).name("Pista 2").active(false).build();

            byte[] pdf = exporter.exportTournamentData(tournament, null, List.of(activeCourt, inactiveCourt), List.of());

            assertThat(pdf).isNotNull();
        }

        @Test
        @DisplayName("should generate PDF for all tournament statuses")
        void should_generate_pdf_for_all_statuses() {
            for (TournamentStatus status : TournamentStatus.values()) {
                Tournament tournament = Tournament.builder()
                        .id(UUID.randomUUID())
                        .name("Torneo " + status)
                        .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                        .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                        .surface(Surface.CLAY)
                        .maxPlayers(32)
                        .location("Madrid")
                        .state(status)
                        .build();

                byte[] pdf = exporter.exportTournamentData(tournament, null, List.of(), List.of());

                assertThat(pdf).isNotNull();
                assertThat(pdf.length).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("should generate PDF for all surfaces")
        void should_generate_pdf_for_all_surfaces() {
            for (Surface surface : Surface.values()) {
                Tournament tournament = Tournament.builder()
                        .id(UUID.randomUUID())
                        .name("Torneo " + surface)
                        .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                        .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                        .surface(surface)
                        .maxPlayers(32)
                        .location("Madrid")
                        .state(TournamentStatus.OPEN)
                        .build();

                byte[] pdf = exporter.exportTournamentData(tournament, null, List.of(), List.of());

                assertThat(pdf).isNotNull();
            }
        }

        @Test
        @DisplayName("should handle null startTime")
        void should_handle_null_start_time() {
            Tournament tournament = Tournament.builder()
                    .id(UUID.randomUUID())
                    .name("Torneo sin hora")
                    .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                    .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                    .surface(Surface.HARD)
                    .maxPlayers(16)
                    .location("Barcelona")
                    .state(TournamentStatus.DRAFT)
                    .startTime(null)
                    .build();

            byte[] pdf = exporter.exportTournamentData(tournament, null, List.of(), List.of());

            assertThat(pdf).isNotNull();
        }

        @Test
        @DisplayName("should handle null locationFormattedAddress")
        void should_handle_null_formatted_address() {
            Tournament tournament = Tournament.builder()
                    .id(UUID.randomUUID())
                    .name("Torneo sin direccion")
                    .playPeriod(new TournamentPeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10)))
                    .inscriptionPeriod(new TournamentPeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 20)))
                    .surface(Surface.GRASS)
                    .maxPlayers(16)
                    .location("Valencia")
                    .locationFormattedAddress(null)
                    .state(TournamentStatus.IN_PROGRESS)
                    .build();

            byte[] pdf = exporter.exportTournamentData(tournament, null, List.of(), List.of());

            assertThat(pdf).isNotNull();
        }

        @Test
        @DisplayName("should handle inscriptions with null fields")
        void should_handle_inscriptions_with_null_fields() {
            Tournament tournament = buildTournament();
            TournamentInscriptionPlayerView playerWithNulls = new TournamentInscriptionPlayerView(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                    null, null, null,
                    null, null, null,
                    "Carlos", null, null, null, null, null, null, null
            );
            TournamentInscriptionsView inscriptions = new TournamentInscriptionsView(
                    tournament.getId(), null, List.of(), List.of(), List.of(playerWithNulls)
            );

            byte[] pdf = exporter.exportTournamentData(tournament, inscriptions, List.of(), List.of());

            assertThat(pdf).isNotNull();
        }
    }
}
