package com.tfm.tennis_platform.infrastructure.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class FlywayMigrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tennis_test_flyway")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("application.security.jwt.secret-key",
                () -> "test-secret-key-for-integration-tests-that-is-long-enough-for-hmac-sha");
        registry.add("application.email.confirmation.required", () -> "false");
        registry.add("application.email.confirmation.enabled", () -> "false");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void all_migrations_applied_successfully() {
        List<Map<String, Object>> migrations = jdbcTemplate.queryForList(
                "SELECT version, description, type, success " +
                        "FROM flyway_schema_history ORDER BY installed_rank"
        );

        assertFalse(migrations.isEmpty(), "Flyway migrations should have been applied");

        for (Map<String, Object> migration : migrations) {
            assertNotNull(migration.get("version"), "Migration version should not be null");
            assertNotNull(migration.get("description"), "Migration description should not be null");
            assertEquals("SQL", migration.get("type"), "Migration type should be SQL");
            assertEquals(true, migration.get("success"), "Migration should have succeeded");
        }
    }

    @Test
    void core_tables_exist_after_migration() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' ORDER BY table_name",
                String.class
        );

        assertTrue(tables.contains("users"), "users table should exist");
        assertTrue(tables.contains("persons"), "persons table should exist");
        assertTrue(tables.contains("tournaments"), "tournaments table should exist");
        assertTrue(tables.contains("events"), "events table should exist");
        assertTrue(tables.contains("inscriptions"), "inscriptions table should exist");
        assertTrue(tables.contains("stages"), "stages table should exist");
        assertTrue(tables.contains("draws"), "draws table should exist");
        assertTrue(tables.contains("matches"), "matches table should exist");
        assertTrue(tables.contains("participants"), "participants table should exist");
        assertTrue(tables.contains("ref_age_category"), "ref_age_category table should exist");
        assertTrue(tables.contains("pro_players"), "pro_players table should exist");
        assertFalse(tables.contains("categories"), "categories table should NOT exist (removed as dead code)");
        assertFalse(tables.contains("rankings"), "rankings table should NOT exist (removed as dead code)");
        assertFalse(tables.contains("matchups"), "matchups table should NOT exist (removed as dead code)");
    }

    @Test
    void ref_age_category_seeded_data_exists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ref_age_category",
                Integer.class
        );

        assertNotNull(count);
        assertTrue(count > 0, "ref_age_category should have seeded data");
    }

    @Test
    void tournaments_table_has_expected_columns() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_name = 'tournaments' ORDER BY ordinal_position",
                String.class
        );

        assertTrue(columns.contains("id"));
        assertTrue(columns.contains("name"));
        assertTrue(columns.contains("start_date"));
        assertTrue(columns.contains("end_date"));
        assertTrue(columns.contains("state"));
        assertTrue(columns.contains("version"));
        assertTrue(columns.contains("created_by"));
    }

    @Test
    void users_table_has_email_confirmation_columns() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_name = 'users' ORDER BY ordinal_position",
                String.class
        );

        assertTrue(columns.contains("email_verified"));
        assertTrue(columns.contains("email_confirmation_token_hash"));
        assertTrue(columns.contains("email_confirmation_expires_at"));
        assertTrue(columns.contains("token_hash"));
    }
}
