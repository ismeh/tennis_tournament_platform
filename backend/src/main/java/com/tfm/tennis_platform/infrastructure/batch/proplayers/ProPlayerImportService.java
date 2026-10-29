package com.tfm.tennis_platform.infrastructure.batch.proplayers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
public class ProPlayerImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProPlayerImportService.class);
    private static final String IMPORT_SOURCE_NAME = "configured-pro-player-sources";
    private static final String CREATE_STAGE_SQL = """
        CREATE TEMP TABLE pro_players_import_stage (
            puntos INTEGER NOT NULL,
            licencia VARCHAR(20),
            name VARCHAR(255) NOT NULL,
            posicion INTEGER NOT NULL,
            posicion_territorial INTEGER NOT NULL,
            posicion_provincial INTEGER NOT NULL,
            posicion_club INTEGER NOT NULL,
            posicion_edad INTEGER NOT NULL,
            edad VARCHAR(50) NOT NULL,
            nombre_club VARCHAR(255) NOT NULL,
            nombre_provincial VARCHAR(255) NOT NULL,
            nombre_territorial VARCHAR(255) NOT NULL,
            nombre_categoria VARCHAR(255) NOT NULL,
            puntos_otorga INTEGER NOT NULL,
            fecha_nacimiento DATE NOT NULL,
            gender VARCHAR(10) NOT NULL
        ) ON COMMIT DROP
        """;
    private static final String INSERT_STAGE_SQL = """
        INSERT INTO pro_players_import_stage (
            puntos,
            licencia,
            name,
            posicion,
            posicion_territorial,
            posicion_provincial,
            posicion_club,
            posicion_edad,
            edad,
            nombre_club,
            nombre_provincial,
            nombre_territorial,
            nombre_categoria,
            puntos_otorga,
            fecha_nacimiento,
            gender
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String REPLACE_PRO_PLAYERS_SQL = """
        INSERT INTO pro_players (
            puntos,
            licencia,
            name,
            posicion,
            posicion_territorial,
            posicion_provincial,
            posicion_club,
            posicion_edad,
            edad,
            nombre_club,
            nombre_provincial,
            nombre_territorial,
            nombre_categoria,
            puntos_otorga,
            fecha_nacimiento,
            gender
        )
        SELECT
            puntos,
            licencia,
            name,
            posicion,
            posicion_territorial,
            posicion_provincial,
            posicion_club,
            posicion_edad,
            edad,
            nombre_club,
            nombre_provincial,
            nombre_territorial,
            nombre_categoria,
            puntos_otorga,
            fecha_nacimiento,
            gender
        FROM pro_players_import_stage
        ORDER BY gender, posicion
        """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ResourceLoader resourceLoader;
    private final ProPlayerCsvParser csvParser;
    private final ProPlayerImportProperties properties;

    public ProPlayerImportService(
        JdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate,
        ResourceLoader resourceLoader,
        ProPlayerCsvParser csvParser,
        ProPlayerImportProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.resourceLoader = resourceLoader;
        this.csvParser = csvParser;
        this.properties = properties;
    }

    public ProPlayerImportResult importConfiguredSources() {
        List<SourceContent> sources = loadSources();
        String checksum = checksum(sources);
        if (hasSuccessfulImport(checksum)) {
            LOGGER.info("Skipping pro players import because checksum {} was already imported", checksum);
            return new ProPlayerImportResult(false, 0, checksum);
        }

        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> importSources(sources, checksum)));
        } catch (RuntimeException exception) {
            recordFailedImport(checksum, exception);
            throw exception;
        }
    }

    private ProPlayerImportResult importSources(List<SourceContent> sources, String checksum) {
        List<ProPlayerCsvRow> rows = new ArrayList<>();
        for (SourceContent source : sources) {
            rows.addAll(csvParser.parse(source.content(), fallbackGender(source.name())));
        }

        jdbcTemplate.execute(CREATE_STAGE_SQL);
        jdbcTemplate.batchUpdate(
            INSERT_STAGE_SQL,
            rows,
            properties.getBatchSize(),
            (preparedStatement, row) -> {
                preparedStatement.setInt(1, row.points());
                if (row.license() == null) {
                    preparedStatement.setNull(2, Types.VARCHAR);
                } else {
                    preparedStatement.setString(2, row.license());
                }
                preparedStatement.setString(3, row.name());
                preparedStatement.setInt(4, row.position());
                preparedStatement.setInt(5, row.territorialPosition());
                preparedStatement.setInt(6, row.provincialPosition());
                preparedStatement.setInt(7, row.clubPosition());
                preparedStatement.setInt(8, row.agePosition());
                preparedStatement.setString(9, row.age());
                preparedStatement.setString(10, row.clubName());
                preparedStatement.setString(11, row.provincialName());
                preparedStatement.setString(12, row.territorialName());
                preparedStatement.setString(13, row.categoryName());
                preparedStatement.setInt(14, row.awardedPoints());
                preparedStatement.setDate(15, Date.valueOf(row.birthDate()));
                preparedStatement.setString(16, row.gender());
            }
        );

        jdbcTemplate.execute("TRUNCATE TABLE pro_players RESTART IDENTITY");
        jdbcTemplate.execute(REPLACE_PRO_PLAYERS_SQL);
        jdbcTemplate.update(
            "INSERT INTO pro_player_imports (source_name, checksum, row_count, status) VALUES (?, ?, ?, ?)",
            IMPORT_SOURCE_NAME,
            checksum,
            rows.size(),
            "SUCCESS"
        );
        LOGGER.info("Imported {} pro players from {} CSV sources", rows.size(), sources.size());
        return new ProPlayerImportResult(true, rows.size(), checksum);
    }

    private List<SourceContent> loadSources() {
        List<SourceContent> sources = new ArrayList<>();
        for (ProPlayerImportProperties.Source source : properties.getSources()) {
            Resource resource = resourceLoader.getResource(source.getLocation());
            if (!resource.exists()) {
                throw new IllegalArgumentException("Pro players CSV source does not exist: " + source.getLocation());
            }
            try (InputStream inputStream = resource.getInputStream()) {
                sources.add(new SourceContent(source.getName(), source.getLocation(), inputStream.readAllBytes()));
            } catch (IOException exception) {
                throw new IllegalArgumentException("Could not read pro players CSV source: " + source.getLocation(), exception);
            }
        }
        return sources;
    }

    private boolean hasSuccessfulImport(String checksum) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pro_player_imports WHERE source_name = ? AND checksum = ? AND status = ?",
            Integer.class,
            IMPORT_SOURCE_NAME,
            checksum,
            "SUCCESS"
        );
        return count != null && count > 0;
    }

    private void recordFailedImport(String checksum, RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null && message.length() > 4000) {
            message = message.substring(0, 4000);
        }
        try {
            jdbcTemplate.update(
                "INSERT INTO pro_player_imports (source_name, checksum, row_count, status, error_message) VALUES (?, ?, ?, ?, ?)",
                IMPORT_SOURCE_NAME,
                checksum,
                0,
                "FAILED",
                message
            );
        } catch (RuntimeException auditException) {
            exception.addSuppressed(auditException);
        }
    }

    private String checksum(List<SourceContent> sources) {
        MessageDigest digest = sha256();
        for (SourceContent source : sources) {
            digest.update(source.name().getBytes(StandardCharsets.UTF_8));
            digest.update(source.location().getBytes(StandardCharsets.UTF_8));
            digest.update(source.content());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private String fallbackGender(String sourceName) {
        if ("male".equalsIgnoreCase(sourceName)) {
            return "MALE";
        }
        if ("female".equalsIgnoreCase(sourceName)) {
            return "FEMALE";
        }
        return null;
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record SourceContent(String name, String location, byte[] content) {
    }
}
