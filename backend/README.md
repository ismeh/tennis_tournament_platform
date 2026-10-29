# Tennis Tournament Platform - Backend

## Structure
The backend of the Tennis Tournament Platform is built using Java with Spring Boot. It provides RESTful APIs for managing tournaments, players, matches, and scores. The following packages are included:
- domain: Contains domain entities, classes representing the data model. Also includes repository interfaces (ports) for data access.
- application: Contains the application-level logic (use cases/services) that orchestrates the domain models.
- infrastructure: Contains configurations and implementations for data persistence (@Entity, JPA Repositories and adapters), security, and other infrastructural concerns.
    - persistence: 
        - the adapter package contains the implementation of the repository interfaces defined in the domain layer using JPA and the @Entity.
    - controllers: Contains REST controllers that expose the API endpoints and its DTOs for request and response handling.

## Versions

- **Java**: 25
- **Maven**: 3.8+
- **Spring Boot**: 3.x

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher

### Running the Project

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080` by default.

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
