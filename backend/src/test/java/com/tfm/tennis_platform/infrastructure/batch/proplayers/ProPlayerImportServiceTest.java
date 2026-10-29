package com.tfm.tennis_platform.infrastructure.batch.proplayers;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProPlayerImportServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private ProPlayerCsvParser csvParser;
    @Mock
    private ProPlayerImportProperties properties;
    @InjectMocks
    private ProPlayerImportService service;

    @Nested
    class ImportConfiguredSourcesTests {
        @Test
        void should_skip_when_checksum_already_imported() {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("male", "classpath:test.csv");
            when(properties.getSources()).thenReturn(List.of(source));

            Resource resource = new ByteArrayResource("data".getBytes());
            when(resourceLoader.getResource("classpath:test.csv")).thenReturn(resource);

            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(1);

            ProPlayerImportResult result = service.importConfiguredSources();

            assertThat(result.imported()).isFalse();
            assertThat(result.rowCount()).isEqualTo(0);
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_import_when_new_checksum() {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("male", "classpath:test.csv");
            when(properties.getSources()).thenReturn(List.of(source));
            when(properties.getBatchSize()).thenReturn(1000);

            Resource resource = new ByteArrayResource("data".getBytes());
            when(resourceLoader.getResource("classpath:test.csv")).thenReturn(resource);

            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            ProPlayerCsvRow row = new ProPlayerCsvRow(
                    100, "L001", "Nadal", 1, 1, 1, 1, 1, "30+", "Club", "Prov", "Terr", "Cat", 50,
                    java.time.LocalDate.of(1990, 1, 1), "M"
            );
            when(csvParser.parse(any(byte[].class), eq("MALE"))).thenReturn(List.of(row));

            when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            ProPlayerImportResult result = service.importConfiguredSources();

            assertThat(result.imported()).isTrue();
            verify(jdbcTemplate).execute("TRUNCATE TABLE pro_players RESTART IDENTITY");
        }

        @Test
        void should_throw_when_source_does_not_exist() {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("male", "classpath:missing.csv");
            when(properties.getSources()).thenReturn(List.of(source));

            Resource resource = mock(Resource.class);
            when(resourceLoader.getResource("classpath:missing.csv")).thenReturn(resource);
            when(resource.exists()).thenReturn(false);

            assertThatThrownBy(() -> service.importConfiguredSources())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        void should_throw_when_io_exception_reading_source() throws Exception {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("male", "classpath:test.csv");
            when(properties.getSources()).thenReturn(List.of(source));

            Resource resource = mock(Resource.class);
            when(resourceLoader.getResource("classpath:test.csv")).thenReturn(resource);
            when(resource.exists()).thenReturn(true);
            when(resource.getInputStream()).thenThrow(new java.io.IOException("read error"));

            assertThatThrownBy(() -> service.importConfiguredSources())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Could not read");
        }

        @Test
        void should_record_failed_import_on_exception() {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("male", "classpath:test.csv");
            when(properties.getSources()).thenReturn(List.of(source));

            Resource resource = new ByteArrayResource("data".getBytes());
            when(resourceLoader.getResource("classpath:test.csv")).thenReturn(resource);

            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            when(transactionTemplate.execute(any(TransactionCallback.class)))
                    .thenThrow(new RuntimeException("import failed"));

            assertThatThrownBy(() -> service.importConfiguredSources())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("import failed");

            verify(jdbcTemplate).update(
                    contains("INSERT INTO pro_player_imports"),
                    eq("configured-pro-player-sources"),
                    anyString(),
                    eq(0),
                    eq("FAILED"),
                    eq("import failed")
            );
        }

        @Test
        void should_truncate_long_error_message() {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("male", "classpath:test.csv");
            when(properties.getSources()).thenReturn(List.of(source));

            Resource resource = new ByteArrayResource("data".getBytes());
            when(resourceLoader.getResource("classpath:test.csv")).thenReturn(resource);

            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            String longMessage = "x".repeat(5000);
            when(transactionTemplate.execute(any(TransactionCallback.class)))
                    .thenThrow(new RuntimeException(longMessage));

            assertThatThrownBy(() -> service.importConfiguredSources())
                    .isInstanceOf(RuntimeException.class);

            verify(jdbcTemplate).update(
                    anyString(),
                    anyString(),
                    anyString(),
                    eq(0),
                    eq("FAILED"),
                    argThat(msg -> msg.toString().length() <= 4000)
            );
        }
    }

    @Nested
    class FallbackGenderTests {
        @Test
        @SuppressWarnings("unchecked")
        void should_return_male_for_male_source() {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("male", "classpath:test.csv");
            when(properties.getSources()).thenReturn(List.of(source));
            when(properties.getBatchSize()).thenReturn(1000);

            Resource resource = new ByteArrayResource("data".getBytes());
            when(resourceLoader.getResource("classpath:test.csv")).thenReturn(resource);

            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            when(csvParser.parse(any(byte[].class), eq("MALE"))).thenReturn(List.of());

            when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            service.importConfiguredSources();

            verify(csvParser).parse(any(byte[].class), eq("MALE"));
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_return_female_for_female_source() {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("female", "classpath:test.csv");
            when(properties.getSources()).thenReturn(List.of(source));
            when(properties.getBatchSize()).thenReturn(1000);

            Resource resource = new ByteArrayResource("data".getBytes());
            when(resourceLoader.getResource("classpath:test.csv")).thenReturn(resource);

            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            when(csvParser.parse(any(byte[].class), eq("FEMALE"))).thenReturn(List.of());

            when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            service.importConfiguredSources();

            verify(csvParser).parse(any(byte[].class), eq("FEMALE"));
        }

        @Test
        @SuppressWarnings("unchecked")
        void should_return_null_for_unknown_source() {
            ProPlayerImportProperties.Source source = new ProPlayerImportProperties.Source("unknown", "classpath:test.csv");
            when(properties.getSources()).thenReturn(List.of(source));
            when(properties.getBatchSize()).thenReturn(1000);

            Resource resource = new ByteArrayResource("data".getBytes());
            when(resourceLoader.getResource("classpath:test.csv")).thenReturn(resource);

            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any(), any()))
                    .thenReturn(0);

            when(csvParser.parse(any(byte[].class), isNull())).thenReturn(List.of());

            when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });

            service.importConfiguredSources();

            verify(csvParser).parse(any(byte[].class), isNull());
        }
    }
}
