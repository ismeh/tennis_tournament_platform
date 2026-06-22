# Tennis Tournament Platform - Backend

Spring Boot backend for tournament, participant, authentication, and match APIs.

## Architecture and Structure

The backend follows a ports-and-adapters (hexagonal) style:

- `domain/`: core entities and repository contracts (ports)
- `application/`: services and use-case orchestration
- `infrastructure/`: adapters (REST controllers, persistence, security, configuration)
  - `persistence/`: JPA entities, repositories, and adapters
  - `controller/`: HTTP endpoints and request/response DTOs

## Supported Versions

| Tool | Version |
|---|---|
| Java | 25 |
| Maven | 3.8+ |
| Spring Boot | 4.0.0 |

> The project is configured for Java 25 (`<java.version>25</java.version>` in `pom.xml`).

## Getting Started (Local)

### Prerequisites

- Java 25 installed and active in your shell
- Maven 3.8+ (or use `./mvnw`)
- PostgreSQL configured (or run via Docker Compose from project root)

### Build, Test, Run

```bash
cd backend
./mvnw clean install
./mvnw test
./mvnw spring-boot:run
```

API URL by common local setup:

- Docker Compose or explicit `BACKEND_PORT=8080`: `http://localhost:8080/api`
- Plain local `dev` profile default (`BACKEND_PORT` unset): `http://localhost:8085/api`
- Plain local `devH2` profile default (`BACKEND_PORT` unset): `http://localhost:8080/api`
- The `dev` profile requires `DATASOURCE_URL`, `DB_USER`, and `DB_PASSWORD` to be set; use `BACKEND_PROFILE=devH2` for a local H2 setup.

All active HTTP controllers should be namespaced under `/api`; see [`../docs/api-spec.md`](../docs/api-spec.md) before changing any API contract.

## Useful Commands

```bash
# Run all tests
./mvnw test

# Package without tests
./mvnw clean package -DskipTests

# Run app
./mvnw spring-boot:run
```

## Testing

The backend includes comprehensive test coverage across multiple layers:

### Test Types

**Unit Tests** (`src/test/java/.../unit/`):
- Pure Java tests with no external dependencies
- Fast execution for domain logic, services, and utilities
- Example: `TournamentServiceTest`, `DrawTest`, `SetScoreTest`

**Integration Tests** (`src/test/java/.../integration/`):
- Spring Boot tests with H2 in-memory database
- MockMvc for HTTP endpoint testing
- JWT security validation
- Flyway migration verification

**API Contract Tests** (`src/test/java/.../docs/`):
- REST Docs documentation generation
- Request/response schema validation
- API specification maintenance

**Performance Tests** (`src/gatling/java/`):
- Gatling load testing simulations
- Tournament workflow stress testing
- Concurrent user scenario validation

### Running Tests

```bash
# All tests (unit + integration + docs)
./mvnw test

# Specific test categories
./mvnw test -Dtest="*Test"           # Unit tests only
./mvnw test -Dtest="*IT"             # Integration tests only
./mvnw test -Dtest="*DocumentationTest"  # API contract tests only

# Single test class
./mvnw test -Dtest="TournamentServiceTest"
./mvnw test -Dtest="LoginControllerIT"

# Single test method
./mvnw test -Dtest="TournamentServiceTest#shouldCreateTournament"

# With coverage
./mvnw test jacoco:report
```

### Test Configuration

**H2 Database** (`application-integration-test.yaml`):
- In-memory database for fast test execution
- Email confirmation disabled for easier testing
- Pre-configured JWT secret for token generation

**Test Data**:
- Each test class uses unique identifiers (UUID-based emails)
- Tests are isolated and don't depend on execution order
- Shared Spring context across test classes for performance

### Writing New Tests

**For new controllers**:
1. Extend `IntegrationTestBase.java`
2. Use `@AutoConfigureMockMvc` for endpoint testing
3. Generate unique test data with `UUID.randomUUID()`

**For new services**:
1. Create unit tests in `src/test/java/.../unit/`
2. Mock dependencies with Mockito
3. Focus on business logic validation

**For API changes**:
1. Update `ApiContractDocumentationTest.java`
2. Add field descriptions for new response properties
3. Run `./mvnw test -Dtest="*DocumentationTest"` to verify

### Test Metrics

Current coverage (116 total tests):
- **79 Unit Tests**: Domain, services, utilities
- **29 Integration Tests**: Controllers, security, migrations
- **4 API Contract Tests**: REST documentation
- **4 Performance Tests**: Gatling simulations (not counted in Maven)

### Troubleshooting Tests

**Common issues**:
- **Port conflicts**: Integration tests use MockMvc (no actual HTTP server)
- **Database state**: Tests use H2 in-memory; no cleanup needed
- **JWT tokens**: Use test secret from `application-integration-test.yaml`
- **Flyway migrations**: Run automatically before integration tests

## Observability

The backend includes Actuator and Micrometer metrics for service execution monitoring.

Service-layer methods are traced by `ServiceObservabilityAspect`, which records:

- `service.start`, `service.success`, and `service.failure` logs with operation, duration, result summary, and exception type.
- `backend.service.execution` timer tagged by `application`, `service`, `method`, `outcome`, and `exception`.
- `backend.service.failures` counter tagged by `application`, `service`, `method`, and `exception`.

Available endpoints:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`
- `GET /actuator/metrics` requires authentication.

Prometheus can scrape `http://<backend-host>:<port>/actuator/prometheus`.

## API and Documentation

- API specification: [`../docs/api-spec.md`](../docs/api-spec.md)
- Architecture overview: [`../docs/architecture.md`](../docs/architecture.md)
- Domain model: [`../docs/domain-model.md`](../docs/domain-model.md)

## Troubleshooting

- `release version 25 not supported`
  - Your local JDK is older than 25. Switch your Java runtime to JDK 25.
- Database connection errors on startup
  - Verify `DB_*` and `DATASOURCE_URL` values in `.env` (or your shell env vars).
- Port already in use
  - Change `BACKEND_PORT` in `.env` or stop the process using that port.

## Pro Players CSV Import

The backend can import the bundled professional player ranking CSV files into the `pro_players` table. The import is disabled by default and is controlled with environment variables.

Bundled CSV files:

- `src/main/resources/db/seed/pro_players_male.csv`
- `src/main/resources/db/seed/pro_players_female.csv`

The import is idempotent by checksum. If the configured CSV contents have already been imported successfully, the next scheduled execution is skipped. Each execution is tracked in the `pro_player_imports` table.

The import requires Flyway migrations to run first because the `pro_players` and `pro_player_imports` tables are created by `db/migration/V6__create_pro_players_import_tables.sql`. In Docker, keep `FLYWAY_BASELINE_ON_MIGRATE=true` only when adopting an existing non-empty database that was created before Flyway. For a fresh database, Flyway runs normally from `V1` to the latest migration.

### Variables

| Variable | Default | Description |
| --- | --- | --- |
| `PRO_PLAYERS_IMPORT_ENABLED` | `false` | Enables the scheduled pro players import job. When `false`, no import job is registered. |
| `PRO_PLAYERS_IMPORT_RUN_ON_STARTUP` | `false` | Runs one import when the application starts. Requires `PRO_PLAYERS_IMPORT_ENABLED=true`. |
| `PRO_PLAYERS_IMPORT_CRON` | `0 0 4 * * MON` | Spring cron expression for periodic imports. The default runs every Monday at 04:00. |
| `PRO_PLAYERS_IMPORT_BATCH_SIZE` | `1000` | Number of rows sent per JDBC batch insert. |
| `PRO_PLAYERS_MALE_CSV_LOCATION` | `classpath:db/seed/pro_players_male.csv` | Male ranking CSV location. Supports classpath resources, local files and HTTP(S) URLs supported by Spring `ResourceLoader`. If the CSV omits `GENDER`, rows from this source are imported as `MALE`. |
| `PRO_PLAYERS_FEMALE_CSV_LOCATION` | `classpath:db/seed/pro_players_female.csv` | Female ranking CSV location. Supports classpath resources, local files and HTTP(S) URLs supported by Spring `ResourceLoader`. If the CSV omits `GENDER`, rows from this source are imported as `FEMALE`. |

### CSV format

Expected header:

```csv
PUNTOS,LICENCIA,NAME,POSICION,POSICION TERR,POSICION PROVINCIAL,POSICION CLUB,POSICION EDAD,EDAD,NOMBRE CLUB,NOMBRE PROVINCIAL,NOMBRE TERRITORIAL,NOMBRE CATEGORIA,PUNTOS OTORGA,FECHA NACIMIENTO,GENDER
```

`GENDER` is optional for the configured `male` and `female` sources because it is inferred from the source. All other columns are required. `FECHA NACIMIENTO` must use `dd/MM/yyyy`.

### Import bundled CSVs on startup

Use this for local setup or for loading the bundled CSVs into a fresh environment:

```bash
SPRING_FLYWAY_ENABLED=true \
PRO_PLAYERS_IMPORT_ENABLED=true \
PRO_PLAYERS_IMPORT_RUN_ON_STARTUP=true \
./mvnw spring-boot:run
```

If the database already existed before this migration, make sure Flyway is allowed to adopt it:

```bash
SPRING_FLYWAY_ENABLED=true \
FLYWAY_BASELINE_ON_MIGRATE=true \
FLYWAY_BASELINE_VERSION=0 \
PRO_PLAYERS_IMPORT_ENABLED=true \
PRO_PLAYERS_IMPORT_RUN_ON_STARTUP=true \
./mvnw spring-boot:run
```

### Schedule periodic imports

This keeps the import job enabled and runs it weekly with the default schedule:

```bash
PRO_PLAYERS_IMPORT_ENABLED=true \
PRO_PLAYERS_IMPORT_CRON="0 0 4 * * MON" \
./mvnw spring-boot:run
```

### Update CSVs without redeploying the app

Configure the source locations to point to external files or URLs instead of the bundled classpath files:

```bash
PRO_PLAYERS_IMPORT_ENABLED=true \
PRO_PLAYERS_MALE_CSV_LOCATION=https://example.com/ranking_male.csv \
PRO_PLAYERS_FEMALE_CSV_LOCATION=https://example.com/ranking_female.csv \
./mvnw spring-boot:run
```

For mounted files, use `file:` URLs:

```bash
PRO_PLAYERS_IMPORT_ENABLED=true \
PRO_PLAYERS_MALE_CSV_LOCATION=file:/data/pro_players_male.csv \
PRO_PLAYERS_FEMALE_CSV_LOCATION=file:/data/pro_players_female.csv \
./mvnw spring-boot:run
```
