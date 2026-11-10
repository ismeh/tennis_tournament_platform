package com.tfm.tennis_platform.infrastructure.excel;

import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Inscription;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.TournamentPeriod;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionPlayerView;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TournamentExcelExporter")
class TournamentExcelExporterTest {

    private TournamentExcelExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new TournamentExcelExporter();
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
                .setsPerMatch(3)
                .gamesPerSet(6)
                .decisiveTiebreakPoints(7)
                .state(TournamentStatus.IN_PROGRESS)
                .build();
    }

    private TournamentInscriptionPlayerView player(UUID inscriptionId, String firstName, String lastName) {
        return new TournamentInscriptionPlayerView(
                inscriptionId, UUID.randomUUID(), UUID.randomUUID(), 1,
                "Absoluto", "Individual masculino", "MALE",
                UUID.randomUUID(), "EXISTING_PERSON", "LIC-001",
                firstName, lastName, "MALE", 120, 1, "Club Central", "CONFIRMED", "PAID"
        );
    }

    private TournamentInscriptionsView inscriptions(Tournament tournament, TournamentInscriptionPlayerView... players) {
        return new TournamentInscriptionsView(
                tournament.getId(), null, List.of(), List.of(), List.of(players)
        );
    }

    private String cellText(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        return row.getCell(columnIndex) != null ? row.getCell(columnIndex).toString() : null;
    }

    private String findValueByLabel(Sheet sheet, String label) {
        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if (cell != null && label.equals(cell.toString())) {
                Cell value = row.getCell(1);
                return value != null ? value.toString() : null;
            }
        }
        return null;
    }

    @Nested
    @DisplayName("exportTournamentData")
    class ExportTournamentDataTests {

        @Test
        @DisplayName("should generate a workbook with one sheet per data set")
        void should_generate_workbook_with_all_sheets() throws Exception {
            Tournament tournament = buildTournament();
            byte[] bytes = exporter.exportTournamentData(tournament, inscriptions(tournament), List.of(), List.of());

            assertThat(bytes).isNotEmpty();
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
                assertThat(workbook.getSheet("Resumen")).isNotNull();
                assertThat(workbook.getSheet("Inscritos")).isNotNull();
                assertThat(workbook.getSheet("Partidos")).isNotNull();
                assertThat(workbook.getSheet("Pistas")).isNotNull();
            }
        }

        @Test
        @DisplayName("should include tournament information in the summary sheet")
        void should_include_summary_information() throws Exception {
            Tournament tournament = buildTournament();

            byte[] bytes = exporter.exportTournamentData(tournament, inscriptions(tournament), List.of(), List.of());

            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet summary = workbook.getSheet("Resumen");
                assertThat(cellText(summary, 0, 0)).isEqualTo("Open de Primavera 2026");
                assertThat(findValueByLabel(summary, "Estado")).isEqualTo("En curso");
                assertThat(findValueByLabel(summary, "Superficie")).isEqualTo("Tierra batida");
                assertThat(findValueByLabel(summary, "Ubicación")).isEqualTo("Madrid");
                assertThat(findValueByLabel(summary, "Capacidad máxima")).isEqualTo("32 jugadores");
                assertThat(findValueByLabel(summary, "Formato")).contains("Mejor de 3 sets");
                assertThat(findValueByLabel(summary, "Inscritos")).isEqualTo("0");
            }
        }

        @Test
        @DisplayName("should write one row per registered player")
        void should_write_inscriptions() throws Exception {
            Tournament tournament = buildTournament();
            TournamentInscriptionPlayerView player = player(UUID.randomUUID(), "Carlos", "Garcia");

            byte[] bytes = exporter.exportTournamentData(tournament, inscriptions(tournament, player), List.of(), List.of());

            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet sheet = workbook.getSheet("Inscritos");
                assertThat(cellText(sheet, 0, 1)).isEqualTo("Jugador");
                assertThat(cellText(sheet, 1, 1)).isEqualTo("Carlos Garcia");
                assertThat(cellText(sheet, 1, 2)).isEqualTo("Individual masculino");
                assertThat(sheet.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(120d);
                assertThat(cellText(sheet, 1, 9)).isEqualTo("Pagado");
            }
        }

        @Test
        @DisplayName("should write match results and placeholders for pending participants")
        void should_write_results_and_placeholders() throws Exception {
            Tournament tournament = buildTournament();
            UUID drawId = UUID.randomUUID();
            UUID firstInscriptionId = UUID.randomUUID();
            UUID secondInscriptionId = UUID.randomUUID();
            UUID thirdInscriptionId = UUID.randomUUID();
            UUID fourthInscriptionId = UUID.randomUUID();

            TournamentInscriptionPlayerView first = player(firstInscriptionId, "Carlos", "Garcia");
            TournamentInscriptionPlayerView second = player(secondInscriptionId, "Rafael", "Nadal");
            TournamentInscriptionPlayerView third = player(thirdInscriptionId, "Roger", "Federer");
            TournamentInscriptionPlayerView fourth = player(fourthInscriptionId, "Novak", "Djokovic");

            Match finalMatch = Match.builder()
                    .id(UUID.randomUUID())
                    .drawId(drawId)
                    .roundNumber(2)
                    .bracketPosition(1)
                    .status(MatchStatus.PENDING)
                    .scheduledAt(LocalDateTime.of(2026, 5, 10, 12, 0))
                    .court("Pista Central")
                    .build();

            Match semifinalOne = Match.builder()
                    .id(UUID.randomUUID())
                    .drawId(drawId)
                    .roundNumber(1)
                    .bracketPosition(1)
                    .firstInscription(Inscription.builder().id(firstInscriptionId).build())
                    .secondInscription(Inscription.builder().id(secondInscriptionId).build())
                    .winner(Inscription.builder().id(firstInscriptionId).build())
                    .nextMatch(finalMatch)
                    .status(MatchStatus.COMPLETED)
                    .result("6-4 6-3")
                    .scheduledAt(LocalDateTime.of(2026, 5, 9, 10, 30))
                    .court("Pista 1")
                    .build();

            Match semifinalTwo = Match.builder()
                    .id(UUID.randomUUID())
                    .drawId(drawId)
                    .roundNumber(1)
                    .bracketPosition(2)
                    .firstInscription(Inscription.builder().id(thirdInscriptionId).build())
                    .secondInscription(Inscription.builder().id(fourthInscriptionId).build())
                    .nextMatch(finalMatch)
                    .status(MatchStatus.PENDING)
                    .scheduledAt(LocalDateTime.of(2026, 5, 9, 12, 0))
                    .court("Pista 2")
                    .build();

            byte[] bytes = exporter.exportTournamentData(
                    tournament,
                    inscriptions(tournament, first, second, third, fourth),
                    List.of(),
                    List.of(finalMatch, semifinalOne, semifinalTwo)
            );

            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet sheet = workbook.getSheet("Partidos");
                assertThat(cellText(sheet, 0, 1)).isEqualTo("Prueba");
                assertThat(cellText(sheet, 1, 2)).isEqualTo("Semifinales");
                assertThat(cellText(sheet, 1, 3)).isEqualTo("Carlos Garcia");
                assertThat(cellText(sheet, 1, 4)).isEqualTo("Rafael Nadal");
                assertThat(cellText(sheet, 1, 9)).isEqualTo("6-4 6-3");
                assertThat(cellText(sheet, 1, 10)).isEqualTo("Carlos Garcia");

                // The final is sorted after both semifinals and its slots are unknown.
                assertThat(cellText(sheet, 3, 2)).isEqualTo("Final");
                assertThat(cellText(sheet, 3, 3)).isEqualTo("Ganador de P-001");
                assertThat(cellText(sheet, 3, 4)).isEqualTo("Ganador de P-002");
            }
        }

        @Test
        @DisplayName("should handle null inscriptions, courts and matches")
        void should_handle_null_data() throws Exception {
            Tournament tournament = buildTournament();

            byte[] bytes = exporter.exportTournamentData(tournament, null, null, null);

            assertThat(bytes).isNotEmpty();
            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
                assertThat(cellText(workbook.getSheet("Inscritos"), 1, 0))
                        .isEqualTo("No hay inscripciones registradas.");
                assertThat(cellText(workbook.getSheet("Partidos"), 1, 0))
                        .isEqualTo("No hay partidos generados todavía.");
                assertThat(cellText(workbook.getSheet("Pistas"), 1, 0))
                        .isEqualTo("No hay pistas configuradas.");
            }
        }

        @Test
        @DisplayName("should write court activity state")
        void should_write_courts() throws Exception {
            Tournament tournament = buildTournament();
            Court active = Court.builder().id(UUID.randomUUID()).tournamentId(tournament.getId()).name("Pista Central").active(true).build();
            Court inactive = Court.builder().id(UUID.randomUUID()).tournamentId(tournament.getId()).name("Pista 2").active(false).build();

            byte[] bytes = exporter.exportTournamentData(tournament, null, List.of(active, inactive), null);

            try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet sheet = workbook.getSheet("Pistas");
                assertThat(cellText(sheet, 1, 1)).isEqualTo("Pista Central");
                assertThat(cellText(sheet, 1, 2)).isEqualTo("Activa");
                assertThat(cellText(sheet, 2, 1)).isEqualTo("Pista 2");
                assertThat(cellText(sheet, 2, 2)).isEqualTo("Inactiva");
            }
        }

        @Test
        @DisplayName("should require a tournament")
        void should_require_tournament() {
            assertThatThrownBy(() -> exporter.exportTournamentData(null, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("torneo");
        }
    }
}
