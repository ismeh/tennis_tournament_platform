# Coding Standards

Coding conventions and style rules for the Tennis Tournament Platform.
All agents and contributors must follow these standards.

---

## General

- **Language of code:** English (class names, method names, variable names, comments).
- **Language of domain / UI strings:** Spanish (route paths like `/torneos`, UI labels).
- **No hardcoded secrets** — use environment variables or Spring properties with `${VAR:default}` syntax.
- **No commented-out dead code** left in submitted code. Use descriptive TODO comments with context if needed.

---

## Backend (Java / Spring Boot)

### Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `TournamentService` |
| Interfaces | PascalCase | `TournamentRepository` |
| Methods | camelCase | `findById`, `createTournament` |
| Variables | camelCase | `tournamentList` |
| Constants | UPPER_SNAKE_CASE | `MAX_PARTICIPANTS` |
| Packages | lowercase, dot-separated | `com.tfm.tennis_platform.application.services` |
| Test methods | snake_case describing behavior | `should_return_tournament_when_found_by_id` |

### Class Structure

Order within a class:
1. Static constants
2. Instance fields (Lombok-annotated or declared)
3. Constructors (prefer `@RequiredArgsConstructor`)
4. Public methods
5. Private methods

### Lombok Usage

- Use `@RequiredArgsConstructor` for constructor injection in Spring beans.
- Use `@Builder` for domain model construction.
- Use `@Value` or `@Data` for DTOs and value objects.
- Never manually write getters/setters for Lombok-annotated classes.

### MapStruct Usage

- Every layer boundary has a dedicated mapper: `*PersistenceMapper` and `*WebMapper`.
- Mappers are Spring-managed (`@Mapper(componentModel = "spring")`).
- Never write manual mapping code where a MapStruct mapper can be used.

### Method Length

- Service and use-case methods: max ~20 lines. Extract to private methods if needed.
- Controller methods: max ~10 lines. Only delegate.

### Error Handling

- Domain: throw `ResourceNotFoundException` for missing entities, `BusinessRuleException` for rule violations.
- Controllers: let Spring's exception resolver handle domain exceptions; do not catch and swallow.
- Do not use `e.printStackTrace()` — use SLF4J: `log.error("message", e)`.

### Transactions

- Annotate service methods with `@Transactional` when they perform multiple write operations.
- Do not annotate read-only methods unless explicitly needed; or use `@Transactional(readOnly = true)`.

### Imports

- No wildcard imports (`import java.util.*`).
- Organize: standard Java → third-party → project-internal.

---

## Frontend (Angular / TypeScript)

### Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Components | PascalCase class + kebab-case selector | `TournamentListComponent`, `app-tournament-list` |
| Services | PascalCase + `Service` suffix | `TournamentService` |
| Interfaces | PascalCase | `TournamentResponse` |
| Files | kebab-case | `tournament-list.ts`, `tournament.service.ts` |
| Methods | camelCase | `loadTournaments()` |
| Signals / observables | `$` suffix for observables | `tournaments$` |

### Component Rules

- All components must be `standalone: true`.
- Use `inject()` function for dependency injection — no constructor parameters.
- Keep templates focused; extract complex logic to the component class.
- No direct DOM manipulation (`document.getElementById`, etc.) — use Angular templates and directives.
- No direct `window`, `localStorage`, or `sessionStorage` access — use `AuthService` or a platform-guarded service.
- SSR guard: wrap platform-sensitive code with `isPlatformBrowser(this.platformId)`.

### Template Style

- One binding per line for readability in multi-attribute elements.
- `trackBy` is required on all `*ngFor` loops over lists with 3+ items.
- Async pipe preferred over manual `.subscribe()` in component lifecycle hooks.

### Services

- HTTP services live in `data/services/`.
- One service per domain entity (e.g., `TournamentService`, `MatchService`).
- Return `Observable<T>` — do not subscribe inside services.
- Handle HTTP errors in the service using `catchError` and rethrowing a user-friendly error.

### Formatter (Prettier)

```json
{
  "printWidth": 100,
  "singleQuote": true,
  "parser (html files)": "angular"
}
```

Run before commit: `npx prettier --write src/`.

---

## Testing Standards

### Backend

- Test class name: `<ClassName>Test` in the same package under `src/test/java`.
- Use `@ExtendWith(MockitoExtension.class)` for unit tests.
- Use `@WebMvcTest` for controller integration tests.
- Use `@DataJpaTest` for repository tests.
- Mock all external dependencies; never use a real DB in a unit test.

### Frontend

- Test file: `<component-name>.spec.ts` co-located with the component.
- Use `TestBed.configureTestingModule` with `standalone: true` components.
- Use `jasmine.createSpyObj` to mock services.
- Do not test auto-generated methods or framework internals.

---

## Git Conventions

- Commit messages: imperative mood, present tense — `Add tournament filter by surface`.
- Branch names: `feature/<short-description>`, `bugfix/<short-description>`, `refactor/<short-description>`.
- Working branch: `develop`. Default branch: `main`.
- Do not commit directly to `main`.
