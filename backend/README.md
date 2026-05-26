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
- The `dev` profile requires `DATASOURCE_URL`, `DB_USER`, and `DB_PASSWORD` to be set; use `devH2` for a local H2 setup.

## Useful Commands

```bash
# Run all tests
./mvnw test

# Package without tests
./mvnw clean package -DskipTests

# Run app
./mvnw spring-boot:run
```

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