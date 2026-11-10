package com.tfm.tennis_platform.infrastructure.excel;

import com.tfm.tennis_platform.domain.models.Court;
import com.tfm.tennis_platform.domain.models.Match;
import com.tfm.tennis_platform.domain.models.Tournament;
import com.tfm.tennis_platform.domain.models.enums.MatchStatus;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionPlayerView;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Generates a multi-sheet Excel workbook with the full tournament snapshot.
 *
 * <p>The workbook contains a summary sheet, the registered players, the complete
 * match list (with results) and the courts. Matches whose participants are not
 * decided yet are rendered with readable placeholders such as
 * {@code Ganador de P-003}, so the sheet remains useful while the tournament is
 * still in progress.</p>
 */
@Component
public class TournamentExcelExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String SHEET_SUMMARY = "Resumen";
    private static final String SHEET_INSCRIPTIONS = "Inscritos";
    private static final String SHEET_MATCHES = "Partidos";
    private static final String SHEET_COURTS = "Pistas";

    private static final String[] MATCH_HEADERS = {
            "Partido", "Prueba", "Ronda", "Jugador 1", "Jugador 2",
            "Fecha", "Hora", "Pista", "Estado", "Resultado", "Ganador"
    };

    public byte[] exportTournamentData(
            Tournament tournament,
            TournamentInscriptionsView inscriptions,
            List<Court> courts,
            List<Match> matches
    ) {
        Objects.requireNonNull(tournament, "El torneo es obligatorio para exportar a Excel.");

        List<Court> safeCourts = courts != null ? courts : List.of();
        List<Match> safeMatches = matches != null ? matches : List.of();
        List<TournamentInscriptionPlayerView> players = inscriptions != null && inscriptions.inscriptions() != null
                ? inscriptions.inscriptions()
                : List.of();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.getProperties().getCoreProperties().setCreator("PuntoMatch");
            workbook.getProperties().getCoreProperties().setTitle("Exportación del torneo " + tournament.getName());

            Styles styles = new Styles(workbook);

            writeSummarySheet(workbook, styles, tournament, players, safeCourts, safeMatches);
            writeInscriptionsSheet(workbook, styles, players);
            writeMatchesSheet(workbook, styles, inscriptions, safeMatches);
            writeCourtsSheet(workbook, styles, safeCourts);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel del torneo", e);
        }
    }

    // ------------------------------------------------------------------
    // Summary
    // ------------------------------------------------------------------

    private void writeSummarySheet(
            XSSFWorkbook workbook,
            Styles styles,
            Tournament tournament,
            List<TournamentInscriptionPlayerView> players,
            List<Court> courts,
            List<Match> matches
    ) {
        Sheet sheet = workbook.createSheet(SHEET_SUMMARY);
        sheet.setColumnWidth(0, 8_000);
        sheet.setColumnWidth(1, 15_000);
        sheet.setColumnWidth(2, 5_000);
        sheet.setColumnWidth(3, 6_000);

        int rowIndex = 0;

        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(tournament.getName());
        titleCell.setCellStyle(styles.title());
        sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 3));
        titleRow.setHeightInPoints(26);

        Row subtitleRow = sheet.createRow(rowIndex++);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Resumen del torneo");
        subtitleCell.setCellStyle(styles.subtitle());
        sheet.addMergedRegion(new CellRangeAddress(subtitleRow.getRowNum(), subtitleRow.getRowNum(), 0, 3));

        rowIndex++;

        rowIndex = addInfoRow(sheet, styles, rowIndex, "Nombre", tournament.getName());
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Estado", getStatusLabel(tournament.getState()));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Superficie", getSurfaceLabel(tournament.getSurface()));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Ubicación", tournament.getLocation());
        if (tournament.getLocationFormattedAddress() != null && !tournament.getLocationFormattedAddress().isBlank()) {
            rowIndex = addInfoRow(sheet, styles, rowIndex, "Dirección", tournament.getLocationFormattedAddress());
        }
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Capacidad máxima", tournament.getMaxPlayers() != null
                ? tournament.getMaxPlayers() + " jugadores"
                : "-");
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Fecha de inicio", formatDate(tournament.getPlayPeriod() != null
                ? tournament.getPlayPeriod().startDate()
                : null));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Fecha de fin", formatDate(tournament.getPlayPeriod() != null
                ? tournament.getPlayPeriod().endDate()
                : null));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Hora de inicio",
                tournament.getStartTime() != null ? tournament.getStartTime().format(TIME_FORMAT) : "-");
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Inscripciones desde", formatDate(tournament.getInscriptionPeriod() != null
                ? tournament.getInscriptionPeriod().startDate()
                : null));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Inscripciones hasta", formatDate(tournament.getInscriptionPeriod() != null
                ? tournament.getInscriptionPeriod().endDate()
                : null));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Formato",
                buildFormatLabel(tournament.getSetsPerMatch(), tournament.getGamesPerSet(), tournament.getDecisiveTiebreakPoints()));

        rowIndex++;
        rowIndex = addSectionTitle(sheet, styles, rowIndex, "Totales");

        long completed = matches.stream().filter(m -> m.getStatus() == MatchStatus.COMPLETED
                || m.getStatus() == MatchStatus.WALKOVER
                || m.getStatus() == MatchStatus.RETIRED).count();
        long pending = matches.size() - completed;

        rowIndex = addInfoRow(sheet, styles, rowIndex, "Inscritos", String.valueOf(players.size()));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Partidos", String.valueOf(matches.size()));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Partidos finalizados", String.valueOf(completed));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Partidos pendientes", String.valueOf(pending));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Pistas", String.valueOf(courts.size()));
        rowIndex = addInfoRow(sheet, styles, rowIndex, "Pistas activas",
                String.valueOf(courts.stream().filter(Court::isActive).count()));

        rowIndex++;
        addInfoRow(sheet, styles, rowIndex, "Fecha de exportación",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private int addInfoRow(Sheet sheet, Styles styles, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(18);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.label());

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null && !value.isBlank() ? value : "-");
        valueCell.setCellStyle(styles.text());

        Cell spacer = row.createCell(2);
        spacer.setCellStyle(styles.text());

        Cell spacer2 = row.createCell(3);
        spacer2.setCellStyle(styles.text());

        return rowIndex + 1;
    }

    private int addSectionTitle(Sheet sheet, Styles styles, int rowIndex, String title) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(styles.section());
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));
        return rowIndex + 1;
    }

    // ------------------------------------------------------------------
    // Inscriptions
    // ------------------------------------------------------------------

    private void writeInscriptionsSheet(
            XSSFWorkbook workbook,
            Styles styles,
            List<TournamentInscriptionPlayerView> players
    ) {
        Sheet sheet = workbook.createSheet(SHEET_INSCRIPTIONS);
        String[] headers = {
                "Nº", "Jugador", "Evento", "Categoría", "Género", "Puntos", "Cabezas de serie",
                "Club", "Estado", "Pago", "Origen", "Licencia"
        };

        int rowIndex = writeHeaderRow(sheet, styles, headers);

        List<TournamentInscriptionPlayerView> sortedPlayers = new ArrayList<>(players);
        sortedPlayers.sort(Comparator
                .comparing((TournamentInscriptionPlayerView p) -> safe(p.eventName()))
                .thenComparing(p -> safe(p.lastName()))
                .thenComparing(p -> safe(p.firstName())));

        int position = 1;
        for (TournamentInscriptionPlayerView player : sortedPlayers) {
            Row row = sheet.createRow(rowIndex++);
            int column = 0;

            writeNumber(row, column++, position++, styles);
            writeText(row, column++, fullName(player), styles);
            writeText(row, column++, player.eventName(), styles);
            writeText(row, column++, player.category(), styles);
            writeText(row, column++, getGenderLabel(player.gender()), styles);
            writeNumber(row, column++, player.points(), styles);
            writeText(row, column++, player.seed() != null ? String.valueOf(player.seed()) : "-", styles);
            writeText(row, column++, player.club(), styles);
            writeText(row, column++, getEntryStatusLabel(player.entryStatus()), styles);
            writeText(row, column++, getPaymentStatusLabel(player.paymentStatus()), styles);
            writeText(row, column++, getPlayerSourceLabel(player.playerSource()), styles);
            writeText(row, column, player.tennisId(), styles);
        }

        if (sortedPlayers.isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            writeText(row, 0, "No hay inscripciones registradas.", styles);
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, headers.length - 1));
        }

        configureTableSheet(sheet, headers.length, rowIndex - 1, 0, true);
        sheet.setColumnWidth(0, 6 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 28 * 256);
        sheet.setColumnWidth(3, 22 * 256);
        sheet.setColumnWidth(4, 12 * 256);
        sheet.setColumnWidth(5, 10 * 256);
        sheet.setColumnWidth(6, 16 * 256);
        sheet.setColumnWidth(7, 24 * 256);
        sheet.setColumnWidth(8, 16 * 256);
        sheet.setColumnWidth(9, 16 * 256);
        sheet.setColumnWidth(10, 18 * 256);
        sheet.setColumnWidth(11, 16 * 256);
    }

    // ------------------------------------------------------------------
    // Matches and placeholders
    // ------------------------------------------------------------------

    private void writeMatchesSheet(
            XSSFWorkbook workbook,
            Styles styles,
            TournamentInscriptionsView inscriptions,
            List<Match> matches
    ) {
        Sheet sheet = workbook.createSheet(SHEET_MATCHES);

        Map<UUID, TournamentInscriptionPlayerView> playersById = indexPlayers(inscriptions);
        List<Match> sortedMatches = sortMatches(matches);

        Map<UUID, String> matchLabels = new HashMap<>();
        for (int i = 0; i < sortedMatches.size(); i++) {
            matchLabels.put(sortedMatches.get(i).getId(), "P-" + String.format("%03d", i + 1));
        }

        Map<UUID, List<Match>> feedersByNextMatch = indexFeeders(sortedMatches);
        Map<UUID, Integer> totalRoundsByDraw = totalRoundsByDraw(sortedMatches);
        Map<UUID, String> eventNameByDraw = indexEventNamesByDraw(sortedMatches, playersById);

        int rowIndex = writeHeaderRow(sheet, styles, MATCH_HEADERS);

        for (Match match : sortedMatches) {
            Row row = sheet.createRow(rowIndex++);
            int column = 0;

            writeText(row, column++, matchLabels.get(match.getId()), styles);
            writeText(row, column++, resolveEventName(match, playersById, eventNameByDraw), styles);
            writeText(row, column++, roundLabel(match, totalRoundsByDraw.get(match.getDrawId())), styles);
            writeText(row, column++, resolvePlayerName(match, true, playersById, feedersByNextMatch, matchLabels), styles);
            writeText(row, column++, resolvePlayerName(match, false, playersById, feedersByNextMatch, matchLabels), styles);

            if (match.getScheduledAt() != null) {
                writeDate(row, column++, match.getScheduledAt().toLocalDate(), styles);
                writeTime(row, column++, match.getScheduledAt().toLocalTime(), styles);
            } else {
                writeText(row, column++, "-", styles);
                writeText(row, column++, "-", styles);
            }

            writeText(row, column++, match.getCourt(), styles);
            writeText(row, column++, getMatchStatusLabel(match.getStatus()), styles);
            writeText(row, column++, match.getResult(), styles);
            writeText(row, column, resolveWinnerName(match, playersById), styles);
        }

        if (sortedMatches.isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            writeText(row, 0, "No hay partidos generados todavía.", styles);
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, MATCH_HEADERS.length - 1));
        }

        configureTableSheet(sheet, MATCH_HEADERS.length, rowIndex - 1, 0, true);
        sheet.getPrintSetup().setLandscape(true);
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 26 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 28 * 256);
        sheet.setColumnWidth(4, 28 * 256);
        sheet.setColumnWidth(5, 12 * 256);
        sheet.setColumnWidth(6, 9 * 256);
        sheet.setColumnWidth(7, 16 * 256);
        sheet.setColumnWidth(8, 14 * 256);
        sheet.setColumnWidth(9, 18 * 256);
        sheet.setColumnWidth(10, 28 * 256);
    }

    private Map<UUID, TournamentInscriptionPlayerView> indexPlayers(TournamentInscriptionsView inscriptions) {
        Map<UUID, TournamentInscriptionPlayerView> playersById = new HashMap<>();
        if (inscriptions != null && inscriptions.inscriptions() != null) {
            for (TournamentInscriptionPlayerView player : inscriptions.inscriptions()) {
                if (player.inscriptionId() != null) {
                    playersById.put(player.inscriptionId(), player);
                }
            }
        }
        return playersById;
    }

    private List<Match> sortMatches(List<Match> matches) {
        List<Match> sorted = new ArrayList<>(matches);
        sorted.sort(Comparator
                .comparing((Match m) -> m.getDrawId() != null ? m.getDrawId().toString() : "")
                .thenComparing(m -> m.getRoundNumber() != null ? m.getRoundNumber() : Integer.MAX_VALUE)
                .thenComparing(m -> m.getBracketPosition() != null ? m.getBracketPosition() : Integer.MAX_VALUE)
                .thenComparing(m -> m.getId() != null ? m.getId().toString() : ""));
        return sorted;
    }

    private Map<UUID, List<Match>> indexFeeders(List<Match> matches) {
        Map<UUID, List<Match>> feeders = new LinkedHashMap<>();
        for (Match match : matches) {
            if (match.getNextMatch() != null && match.getNextMatch().getId() != null) {
                feeders.computeIfAbsent(match.getNextMatch().getId(), key -> new ArrayList<>()).add(match);
            }
        }
        return feeders;
    }

    private Map<UUID, Integer> totalRoundsByDraw(List<Match> matches) {
        Map<UUID, Integer> rounds = new HashMap<>();
        for (Match match : matches) {
            if (match.getDrawId() != null && match.getRoundNumber() != null) {
                rounds.merge(match.getDrawId(), match.getRoundNumber(), Math::max);
            }
        }
        return rounds;
    }

    private String roundLabel(Match match, Integer totalRounds) {
        Integer round = match.getRoundNumber();
        if (round == null) {
            return "-";
        }
        if (totalRounds == null || totalRounds <= 0) {
            return "Ronda " + round;
        }
        int remaining = totalRounds - round;
        return switch (remaining) {
            case 0 -> "Final";
            case 1 -> "Semifinales";
            case 2 -> "Cuartos de final";
            case 3 -> "Octavos de final";
            default -> "Ronda " + round;
        };
    }

    private String resolvePlayerName(
            Match match,
            boolean firstSlot,
            Map<UUID, TournamentInscriptionPlayerView> playersById,
            Map<UUID, List<Match>> feedersByNextMatch,
            Map<UUID, String> matchLabels
    ) {
        UUID inscriptionId = firstSlot ? match.getFirstInscriptionId() : match.getSecondInscriptionId();
        if (inscriptionId != null) {
            TournamentInscriptionPlayerView player = playersById.get(inscriptionId);
            return player != null ? fullName(player) : "Jugador desconocido";
        }

        UUID otherInscriptionId = firstSlot ? match.getSecondInscriptionId() : match.getFirstInscriptionId();
        List<Match> feeders = new ArrayList<>(feedersByNextMatch.getOrDefault(match.getId(), List.of()));

        if (feeders.size() == 2) {
            // If the opposite slot is already decided, the placeholder belongs to the
            // feeder that did not produce that player.
            if (otherInscriptionId != null) {
                Match producingFeeder = feeders.stream()
                        .filter(f -> otherInscriptionId.equals(f.getWinnerId()))
                        .findFirst()
                        .orElse(null);
                if (producingFeeder != null) {
                    Match pendingFeeder = feeders.stream()
                            .filter(f -> f != producingFeeder)
                            .findFirst()
                            .orElse(null);
                    if (pendingFeeder != null) {
                        return "Ganador de " + matchLabels.getOrDefault(pendingFeeder.getId(), "partido pendiente");
                    }
                }
            }
            Match feeder = feeders.get(firstSlot ? 0 : 1);
            return "Ganador de " + matchLabels.getOrDefault(feeder.getId(), "partido pendiente");
        }

        if (feeders.size() == 1) {
            Match feeder = feeders.get(0);
            if (otherInscriptionId == null || !otherInscriptionId.equals(feeder.getWinnerId())) {
                return "Ganador de " + matchLabels.getOrDefault(feeder.getId(), "partido pendiente");
            }
        }

        return "Por definir";
    }

    private String resolveWinnerName(Match match, Map<UUID, TournamentInscriptionPlayerView> playersById) {
        UUID winnerId = match.getWinnerId();
        if (winnerId == null) {
            return "-";
        }
        TournamentInscriptionPlayerView winner = playersById.get(winnerId);
        return winner != null ? fullName(winner) : "-";
    }

    private Map<UUID, String> indexEventNamesByDraw(
            List<Match> matches,
            Map<UUID, TournamentInscriptionPlayerView> playersById
    ) {
        Map<UUID, String> eventNameByDraw = new HashMap<>();
        for (Match match : matches) {
            if (match.getDrawId() == null) {
                continue;
            }
            UUID inscriptionId = match.getFirstInscriptionId() != null
                    ? match.getFirstInscriptionId()
                    : match.getSecondInscriptionId();
            TournamentInscriptionPlayerView player = inscriptionId != null ? playersById.get(inscriptionId) : null;
            if (player != null && player.eventName() != null) {
                eventNameByDraw.putIfAbsent(match.getDrawId(), player.eventName());
            }
        }
        return eventNameByDraw;
    }

    private String resolveEventName(
            Match match,
            Map<UUID, TournamentInscriptionPlayerView> playersById,
            Map<UUID, String> eventNameByDraw
    ) {
        UUID inscriptionId = match.getFirstInscriptionId() != null
                ? match.getFirstInscriptionId()
                : match.getSecondInscriptionId();
        if (inscriptionId != null) {
            TournamentInscriptionPlayerView player = playersById.get(inscriptionId);
            if (player != null && player.eventName() != null) {
                return player.eventName();
            }
        }
        return match.getDrawId() != null
                ? eventNameByDraw.getOrDefault(match.getDrawId(), "-")
                : "-";
    }

    // ------------------------------------------------------------------
    // Courts
    // ------------------------------------------------------------------

    private void writeCourtsSheet(XSSFWorkbook workbook, Styles styles, List<Court> courts) {
        Sheet sheet = workbook.createSheet(SHEET_COURTS);
        String[] headers = {"Nº", "Pista", "Estado"};

        int rowIndex = writeHeaderRow(sheet, styles, headers);

        int position = 1;
        for (Court court : courts) {
            Row row = sheet.createRow(rowIndex++);
            writeNumber(row, 0, position++, styles);
            writeText(row, 1, court.getName(), styles);
            writeText(row, 2, court.isActive() ? "Activa" : "Inactiva", styles);
        }

        if (courts.isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            writeText(row, 0, "No hay pistas configuradas.", styles);
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, headers.length - 1));
        }

        configureTableSheet(sheet, headers.length, rowIndex - 1, 0, false);
        sheet.setColumnWidth(0, 6 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 14 * 256);
    }

    // ------------------------------------------------------------------
    // Low level helpers
    // ------------------------------------------------------------------

    private int writeHeaderRow(Sheet sheet, Styles styles, String... headers) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(22);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header());
        }
        return 1;
    }

    private void configureTableSheet(Sheet sheet, int columns, int lastRowIndex, int headerRowIndex, boolean autoFilter) {
        sheet.createFreezePane(0, headerRowIndex + 1);
        if (autoFilter && lastRowIndex > headerRowIndex) {
            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, lastRowIndex, 0, columns - 1));
        }
        sheet.setRepeatingRows(new CellRangeAddress(headerRowIndex, headerRowIndex, -1, -1));
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private void writeText(Row row, int column, String value, Styles styles) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null && !value.isBlank() ? value : "-");
        cell.setCellStyle(styles.text());
    }

    private void writeNumber(Row row, int column, Integer value, Styles styles) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setCellValue("-");
        } else {
            cell.setCellValue(value);
        }
        cell.setCellStyle(styles.text());
    }

    private void writeDate(Row row, int column, LocalDate value, Styles styles) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setCellValue("-");
            cell.setCellStyle(styles.text());
            return;
        }
        cell.setCellValue(value);
        cell.setCellStyle(styles.date());
    }

    private void writeTime(Row row, int column, LocalTime value, Styles styles) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setCellValue("-");
            cell.setCellStyle(styles.text());
            return;
        }
        cell.setCellValue(value.toSecondOfDay() / 86_400.0d);
        cell.setCellStyle(styles.time());
    }

    private String fullName(TournamentInscriptionPlayerView player) {
        String firstName = safe(player.firstName());
        String lastName = safe(player.lastName());
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? "Jugador sin nombre" : fullName;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String buildFormatLabel(Integer setsPerMatch, Integer gamesPerSet, Integer decisiveTiebreakPoints) {
        String sets = setsPerMatch != null ? "Mejor de " + setsPerMatch + " sets" : "Formato no definido";
        String games = gamesPerSet != null ? gamesPerSet + " juegos por set" : "";
        String tiebreak = decisiveTiebreakPoints != null ? "tiebreak decisivo a " + decisiveTiebreakPoints : "";
        return String.join(" · ", nonBlank(sets, games, tiebreak));
    }

    private List<String> nonBlank(String... values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "-";
    }

    private String getStatusLabel(TournamentStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case DRAFT -> "Borrador";
            case OPEN -> "Inscripciones abiertas";
            case CLOSED -> "Inscripciones cerradas";
            case IN_PROGRESS -> "En curso";
            case COMPLETED -> "Finalizado";
            case CANCELLED -> "Cancelado";
        };
    }

    private String getSurfaceLabel(Surface surface) {
        if (surface == null) {
            return "-";
        }
        return switch (surface) {
            case CLAY -> "Tierra batida";
            case HARD -> "Pista dura";
            case GRASS -> "Césped";
            case CARPET -> "Moqueta";
        };
    }

    private String getGenderLabel(String gender) {
        if (gender == null) {
            return "-";
        }
        return switch (gender.toUpperCase()) {
            case "MALE" -> "Masculino";
            case "FEMALE" -> "Femenino";
            case "MIXED" -> "Mixto";
            default -> gender;
        };
    }

    private String getPlayerSourceLabel(String source) {
        if (source == null) {
            return "-";
        }
        return switch (source) {
            case "EXISTING_PERSON" -> "Jugador existente";
            case "MANUAL" -> "Manual";
            case "PROFESSIONAL" -> "Profesional";
            default -> source;
        };
    }

    private String getEntryStatusLabel(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> "Confirmada";
            case "PENDING" -> "Pendiente";
            case "WAITLIST" -> "Lista de espera";
            case "CANCELLED" -> "Cancelada";
            default -> status;
        };
    }

    private String getPaymentStatusLabel(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status.toUpperCase()) {
            case "PAID" -> "Pagado";
            case "PENDING" -> "Pendiente";
            case "UNPAID" -> "No pagado";
            case "REFUNDED" -> "Reembolsado";
            default -> status;
        };
    }

    private String getMatchStatusLabel(MatchStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case IN_PROGRESS -> "En curso";
            case COMPLETED -> "Finalizado";
            case WALKOVER -> "Walkover";
            case RETIRED -> "Retirada";
            case CANCELLED -> "Cancelado";
            case SUSPENDED -> "Suspendido";
        };
    }

    /**
     * Cell styles are created once per workbook: POI imposes a hard limit on the
     * number of styles, so they must not be created inside the row loops.
     */
    private record Styles(CellStyle title, CellStyle subtitle, CellStyle section, CellStyle label,
                          CellStyle header, CellStyle text, CellStyle date, CellStyle time) {

        Styles(XSSFWorkbook workbook) {
            this(
                    createTitleStyle(workbook),
                    createSubtitleStyle(workbook),
                    createSectionStyle(workbook),
                    createLabelStyle(workbook),
                    createHeaderStyle(workbook),
                    createTextStyle(workbook),
                    createDateStyle(workbook),
                    createTimeStyle(workbook)
            );
        }

        private static CellStyle createTitleStyle(XSSFWorkbook workbook) {
            CellStyle style = baseStyle(workbook);
            Font font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) 18);
            font.setBold(true);
            font.setColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.LEFT);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }

        private static CellStyle createSubtitleStyle(XSSFWorkbook workbook) {
            CellStyle style = baseStyle(workbook);
            Font font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) 11);
            font.setItalic(true);
            font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            style.setFont(font);
            return style;
        }

        private static CellStyle createSectionStyle(XSSFWorkbook workbook) {
            CellStyle style = baseStyle(workbook);
            Font font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);
            font.setColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFont(font);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            return style;
        }

        private static CellStyle createLabelStyle(XSSFWorkbook workbook) {
            CellStyle style = baseStyle(workbook);
            Font font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) 10);
            font.setBold(true);
            font.setColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        }

        private static CellStyle createHeaderStyle(XSSFWorkbook workbook) {
            CellStyle style = baseStyle(workbook);
            Font font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) 10);
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }

        private static CellStyle createTextStyle(XSSFWorkbook workbook) {
            CellStyle style = baseStyle(workbook);
            Font font = workbook.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) 10);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.LEFT);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }

        private static CellStyle createDateStyle(XSSFWorkbook workbook) {
            CellStyle style = createTextStyle(workbook);
            style.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
            return style;
        }

        private static CellStyle createTimeStyle(XSSFWorkbook workbook) {
            CellStyle style = createTextStyle(workbook);
            style.setDataFormat(workbook.createDataFormat().getFormat("hh:mm"));
            return style;
        }

        private static CellStyle baseStyle(XSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            return style;
        }
    }
}
