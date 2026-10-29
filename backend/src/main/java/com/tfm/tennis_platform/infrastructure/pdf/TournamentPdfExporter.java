package com.tfm.tennis_platform.infrastructure.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.tfm.tennis_platform.domain.models.*;
import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionPlayerView;
import com.tfm.tennis_platform.domain.models.inscription.TournamentInscriptionsView;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class TournamentPdfExporter {

    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_HEADER_BG = new Color(241, 245, 249);
    private static final Color COLOR_BORDER = new Color(203, 213, 225);
    private static final Color COLOR_TEXT = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportTournamentData(
            Tournament tournament,
            TournamentInscriptionsView inscriptions,
            List<Court> courts,
            List<Match> matches
    ) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addCoverSection(document, tournament);
            addInscriptionsSection(document, inscriptions);
            addCourtsSection(document, courts);
            addScheduleSection(document, matches, inscriptions);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF del torneo", e);
        }

        return out.toByteArray();
    }

    private void addCoverSection(Document document, Tournament tournament) throws DocumentException {
        Paragraph title = new Paragraph(tournament.getName(), createTitleFont());
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph subtitle = new Paragraph("Datos del Torneo", createSubtitleFont());
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(20);

        addInfoRow(infoTable, "Estado", getStatusLabel(tournament.getState()));
        addInfoRow(infoTable, "Superficie", getSurfaceLabel(tournament.getSurface()));
        addInfoRow(infoTable, "Ubicacion", tournament.getLocation());
        if (tournament.getLocationFormattedAddress() != null) {
            addInfoRow(infoTable, "Direccion", tournament.getLocationFormattedAddress());
        }
        addInfoRow(infoTable, "Capacidad maxima", String.valueOf(tournament.getMaxPlayers()) + " jugadores");
        addInfoRow(infoTable, "Fecha de inicio", formatDate(tournament.getPlayPeriod().startDate()));
        addInfoRow(infoTable, "Fecha de fin", formatDate(tournament.getPlayPeriod().endDate()));
        if (tournament.getStartTime() != null) {
            addInfoRow(infoTable, "Hora de inicio", tournament.getStartTime().toString());
        }
        addInfoRow(infoTable, "Inscripciones desde", formatDate(tournament.getInscriptionPeriod().startDate()));
        addInfoRow(infoTable, "Inscripciones hasta", formatDate(tournament.getInscriptionPeriod().endDate()));

        document.add(infoTable);
    }

    private void addInscriptionsSection(Document document, TournamentInscriptionsView inscriptions) throws DocumentException {
        if (inscriptions == null || inscriptions.inscriptions() == null || inscriptions.inscriptions().isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("Inscripciones", createSectionFont());
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        List<TournamentInscriptionPlayerView> players = inscriptions.inscriptions();

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        float[] columnWidths = {35f, 30f, 20f, 15f};
        table.setWidths(columnWidths);

        addTableHeader(table, "Nombre", "Prueba", "Genero", "Origen");

        for (TournamentInscriptionPlayerView player : players) {
            String fullName = (player.firstName() + " " + (player.lastName() != null ? player.lastName() : "")).trim();
            addTableCell(table, fullName);
            addTableCell(table, player.eventName() != null ? player.eventName() : "");
            addTableCell(table, getGenderLabel(player.gender()));
            addTableCell(table, getPlayerSourceLabel(player.playerSource()));
        }

        document.add(table);

        Paragraph total = new Paragraph("Total inscritos: " + players.size(), createNormalFont());
        total.setSpacingBefore(5);
        total.setSpacingAfter(10);
        document.add(total);
    }

    private void addCourtsSection(Document document, List<Court> courts) throws DocumentException {
        if (courts == null || courts.isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("Pistas", createSectionFont());
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        float[] columnWidths = {60f, 40f};
        table.setWidths(columnWidths);

        addTableHeader(table, "Nombre", "Estado");

        for (Court court : courts) {
            addTableCell(table, court.getName());
            addTableCell(table, court.isActive() ? "Activa" : "Inactiva");
        }

        document.add(table);
    }

    private void addScheduleSection(Document document, List<Match> matches, TournamentInscriptionsView inscriptions) throws DocumentException {
        if (matches == null || matches.isEmpty()) {
            return;
        }

        List<Match> scheduledMatches = matches.stream()
                .filter(m -> m.getScheduledAt() != null)
                .sorted((a, b) -> a.getScheduledAt().compareTo(b.getScheduledAt()))
                .toList();

        if (scheduledMatches.isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("Programacion de Partidos", createSectionFont());
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        float[] columnWidths = {12f, 12f, 25f, 25f, 15f, 11f};
        table.setWidths(columnWidths);

        addTableHeader(table, "Fecha", "Hora", "Jugador 1", "Jugador 2", "Pista", "Resultado");

        for (Match match : scheduledMatches) {
            addTableCell(table, formatDate(match.getScheduledAt().toLocalDate()));
            addTableCell(table, match.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
            addTableCell(table, resolvePlayerName(match.getFirstInscriptionId(), inscriptions));
            addTableCell(table, resolvePlayerName(match.getSecondInscriptionId(), inscriptions));
            addTableCell(table, match.getCourt() != null ? match.getCourt() : "-");
            addTableCell(table, match.getResult() != null ? match.getResult() : "-");
        }

        document.add(table);
    }

    private String resolvePlayerName(UUID inscriptionId, TournamentInscriptionsView inscriptions) {
        if (inscriptionId == null || inscriptions == null || inscriptions.inscriptions() == null) {
            return "Por definir";
        }

        return inscriptions.inscriptions().stream()
                .filter(p -> p.inscriptionId().equals(inscriptionId))
                .map(p -> (p.firstName() + " " + (p.lastName() != null ? p.lastName() : "")).trim())
                .findFirst()
                .orElse("Por definir");
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, createBoldFont()));
        labelCell.setBackgroundColor(COLOR_HEADER_BG);
        labelCell.setBorderColor(COLOR_BORDER);
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", createNormalFont()));
        valueCell.setBorderColor(COLOR_BORDER);
        valueCell.setPadding(8);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, createBoldFont()));
            cell.setBackgroundColor(COLOR_PRIMARY);
            cell.setPhrase(new Phrase(header, createWhiteBoldFont()));
            cell.setBorderColor(COLOR_BORDER);
            cell.setPadding(8);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "-", createNormalFont()));
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String getStatusLabel(TournamentStatus status) {
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
        return switch (surface) {
            case CLAY -> "Tierra batida";
            case HARD -> "Pista dura";
            case GRASS -> "Cesped";
            case CARPET -> "Moqueta";
        };
    }

    private String getGenderLabel(String gender) {
        if (gender == null) return "-";
        return switch (gender.toUpperCase()) {
            case "MALE" -> "Masculino";
            case "FEMALE" -> "Femenino";
            case "MIXED" -> "Mixto";
            default -> gender;
        };
    }

    private String getPlayerSourceLabel(String source) {
        if (source == null) return "-";
        return switch (source) {
            case "EXISTING_PERSON" -> "Jugador existente";
            case "MANUAL" -> "Manual";
            case "PROFESSIONAL" -> "Profesional";
            default -> source;
        };
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "-";
    }

    private Font createTitleFont() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, COLOR_TEXT);
        return font;
    }

    private Font createSubtitleFont() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12, COLOR_TEXT_MUTED);
        return font;
    }

    private Font createSectionFont() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRIMARY);
        return font;
    }

    private Font createBoldFont() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_TEXT);
        return font;
    }

    private Font createWhiteBoldFont() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        return font;
    }

    private Font createNormalFont() {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXT);
        return font;
    }
}