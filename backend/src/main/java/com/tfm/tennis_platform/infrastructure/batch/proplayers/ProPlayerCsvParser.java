package com.tfm.tennis_platform.infrastructure.batch.proplayers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
class ProPlayerCsvParser {

    private static final DateTimeFormatter BIRTH_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    List<ProPlayerCsvRow> parse(byte[] content) {
        return parse(content, null);
    }

    List<ProPlayerCsvRow> parse(byte[] content, String fallbackGender) {
        try (
            InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
            CSVParser csvParser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(reader)
        ) {
            List<ProPlayerCsvRow> rows = new ArrayList<>();
            for (CSVRecord record : csvParser) {
                rows.add(parseRecord(record, fallbackGender));
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read pro players CSV", exception);
        }
    }

    private ProPlayerCsvRow parseRecord(CSVRecord record, String fallbackGender) {
        return new ProPlayerCsvRow(
            integer(record, "PUNTOS"),
            nullableString(record, "LICENCIA"),
            requiredString(record, "NAME"),
            integer(record, "POSICION"),
            integer(record, "POSICION TERR"),
            integer(record, "POSICION PROVINCIAL"),
            integer(record, "POSICION CLUB"),
            integer(record, "POSICION EDAD"),
            requiredString(record, "EDAD"),
            requiredString(record, "NOMBRE CLUB"),
            requiredString(record, "NOMBRE PROVINCIAL"),
            requiredString(record, "NOMBRE TERRITORIAL"),
            requiredString(record, "NOMBRE CATEGORIA"),
            integer(record, "PUNTOS OTORGA"),
            LocalDate.parse(requiredString(record, "FECHA NACIMIENTO"), BIRTH_DATE_FORMAT),
            gender(record, fallbackGender)
        );
    }

    private Integer integer(CSVRecord record, String header) {
        return Integer.valueOf(requiredString(record, header));
    }

    private String requiredString(CSVRecord record, String header) {
        String value = record.get(header).trim();
        if (value.isBlank() || "null".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Missing required CSV value for " + header + " at row " + record.getRecordNumber());
        }
        return value;
    }

    private String nullableString(CSVRecord record, String header) {
        if (!record.isMapped(header) || !record.isSet(header)) {
            return null;
        }
        String value = record.get(header).trim();
        if (value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private String gender(CSVRecord record, String fallbackGender) {
        String csvGender = nullableString(record, "GENDER");
        if (csvGender != null) {
            return csvGender;
        }
        if (fallbackGender == null || fallbackGender.isBlank()) {
            throw new IllegalArgumentException("Missing required CSV value for GENDER at row " + record.getRecordNumber());
        }
        return fallbackGender;
    }
}
