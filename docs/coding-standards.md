# Coding Standards

Coding conventions and style rules for PuntoMatch.
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
| Packages | lowercase, dot-separated | `com.puntomatch.application.services` |
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
- Use **`record`** for DTOs (`TournamentRequest`, `LoginResponse`, ...). Lombok `@Value`/`@Data` on
  DTOs is not the pattern actually in use.
- Never manually write getters/setters for Lombok-annotated classes.

### MapStruct Usage

- Every layer boundary has a dedicated mapper: `*PersistenceMapper` and `*WebMapper`.
- Mappers are Spring-managed (`@Mapper(componentModel = "spring")`).
- Never write manual mapping code where a MapStruct mapper can be used.

### Method Length

- Service and use-case methods: max ~20 lines. Extract to private methods if needed.
- Controller methods: max ~10 lines. Only delegate.

### Error Handling

- Domain: throw `ResourceNotFoundException` for missing entities, and a `BaseException` subclass for
  rule violations. The available subclasses are `DuplicateResourceException`,
  `InvalidArgumentException`, `UnauthorizedException`, `InvalidTokenException` and
  `ExpiredTokenException`, each carrying its own error code and HTTP status.
- `AuthService` follows this pattern. When a plain `IllegalArgumentException` reaches
  `GlobalExceptionHandler` it falls through to the catch-all handler and returns 500, so prefer a
  domain exception whenever the failure has a meaningful client-facing status.
- Controllers: let Spring's exception resolver handle domain exceptions; do not catch and swallow.
  Note that `LoginController` declares its own local `@ExceptionHandler`s, which take precedence for
  that controller.
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
| Components | PascalCase class + `app-` selector | `TournamentsListComponent`, `app-tournaments-list-page` |
| Services | PascalCase + `Service` suffix | `TournamentService` |
| Interfaces / types | PascalCase | `TournamentResponse` |
| Files | kebab-case | `request-logger.service.ts`, `auth.interceptor.ts` |
| Methods | camelCase | `loadTournaments()` |
| Observables | `$` suffix | `authStatus$` |

> The feature-page files are named after their route segment rather than in kebab-case —
> `features/tournaments/list.ts`, `create.ts`, `detail.ts`, `features/auth/login.ts`. Follow the
> existing layout when adding a sibling page rather than renaming files.

### Enum Types

Frontend union types for backend enums must stay in sync by hand — there is no codegen. Currently
drifting:

- `TournamentStatus` (frontend) includes `'ACTIVE'`, which is **not** a backend `TournamentStatus`.
- `TournamentEventGender` (frontend) omits `'OPEN'`, which the SQL schema documents as valid.

### Component Rules

- All components must be `standalone: true`.
- Use `inject()` function for dependency injection — no constructor parameters.
- Keep templates focused; extract complex logic to the component class.
- No direct DOM manipulation (`document.getElementById`, etc.) — use Angular templates and directives.
- No direct `window`, `localStorage`, or `sessionStorage` access outside `AuthService`, which owns
  the token keys (`access_token`, `refresh_token`, `user_display_name`).
- SSR guard: wrap platform-sensitive code with `isPlatformBrowser(this.platformId)`.
- Change detection is **zoneless**. Mutating a plain field will not repaint — expose a `signal` or
  bind through the `async` pipe.

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

Run before commit: `npm run format` (or `npx prettier --write "src/**/*.{html,ts,scss,css,json}"`).
The config lives in the `prettier` key of `package.json`, not a `.prettierrc`.

---

## Testing Standards

### Backend

- Test class name: `<ClassName>Test` in the same package under `src/test/java`.
- Use `@ExtendWith(MockitoExtension.class)` for unit tests, with `@Mock` + `@InjectMocks`.
- Use `@WebMvcTest` for controller integration tests.
- Use `@DataJpaTest` for repository tests.
- Mock all external dependencies; never use a real DB in a unit test.

### Frontend

- Test file: `<name>.spec.ts` co-located with the component.
- Use `TestBed.configureTestingModule` with `standalone: true` components.
- Use `jasmine.createSpyObj` to mock services.
- Do not test auto-generated methods or framework internals.

### Test runner notes

- `npm test` starts Karma in watch mode and does not exit. Use `npm run test:headless` for
  non-interactive runs and `npm run test:ci` for coverage.
- Test method naming is being migrated to snake_case; `TournamentServiceTest` already follows it and
  new tests should match.
- Coverage currently focuses on the service layer; controller, repository and mapper tests are
  welcome additions.

---

## Git Conventions

- Commit messages: imperative mood, present tense — `Add tournament filter by surface`.
- Branch names: `feature/<short-description>`, `bugfix/<short-description>`, `refactor/<short-description>`.
- Working branch: `develop`. Default branch: `main`.
- Do not commit directly to `main`.

---

## Packages to Be Aware Of

A few similarly named locations can cause confusion. These notes describe intent so you edit the
right file:

| Path | Note |
|---|---|
| `backend/.../application/services/` | Main services (plural). New services belong here. |
| `backend/.../application/service/` | Holds `EventService`; singular package name. |
| `backend/.../application/services_use_cases/` | Interface placeholder for the auth use cases. |
| `frontend/src/app/features/auth/login.ts` | The routed `LoginComponent` (`app-login-page`). |
| `frontend/src/app/components/login.ts` | An earlier `LoginComponent` (`app-login`); prefer the feature one. |

`frontend/src/app/components/placeholder-component.ts` is in current use, imported by
`features/placeholder.ts`.

