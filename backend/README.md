# Tennis Tournament Platform - Backend

## Structure
The backend of the Tennis Tournament Platform is built using Java with Spring Boot. It provides RESTful APIs for managing tournaments, players, matches, and scores. The following packages are included:
- domain: Contains entity classes representing the data model. Also includes repository interfaces for data access.
- application: Contains the application-level logic (use cases) that orchestrates the domain models.
- infrastructure: Contains configurations and implementations for data persistence, security, and other infrastructural concerns.

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