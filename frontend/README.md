# Frontend - Tennis Tournament Platform

Angular 20 SSR application for the Tennis Tournament Platform user interface.

## Requirements

- Node.js 24 (recommended)
- npm 10+

Check versions:

```bash
node -v
npm -v
```

## Installation

From the `frontend/` folder:

```bash
npm ci
```

## Run in Development

1. Start the frontend:

```bash
npm run start
```

2. Open in the browser:

- `http://localhost:4200`

Notes:

- `start` uses the `development` configuration defined in `angular.json`.
- The frontend API target is currently `http://localhost:8080/api` (`src/app/shared/constants.ts` and runtime `public/config.json`). If you run backend locally with the default PostgreSQL `dev` profile, either start it with `BACKEND_PORT=8080` or change `public/config.json` to `http://localhost:8085/api`.

## Production Build and SSR Runtime

### 1) Build production assets

```bash
npm run build
```

Artifacts are generated in `dist/tfm_front/`.

### 2) Run SSR server

```bash
npm run serve:ssr:tfm_front
```

Default URL: `http://localhost:4000`

Change port:

```bash
PORT=8080 npm run serve:ssr:tfm_front
```

## Environments and Logging

The project uses native Angular environment files:

- Production: `src/environments/environment.ts`
- Development: `src/environments/environment.development.ts`

Current logger behavior:

- Development:
  - `enableConsole: true`
  - `minLogLevel: INFO`
- Production:
  - `enableConsole: false`
  - `minLogLevel: ERROR`

Environment file replacement is configured in `angular.json` via `fileReplacements`.

## Runtime API Configuration (Local + Docker)

The frontend reads the API URL at runtime from `public/config.json`:

```json
{
	"apiUrl": "http://localhost:8080/api",
	"production": "false"
}
```

- Local (`npm run start`): uses `public/config.json` directly.
- Docker: uses `public/config.template.json` and the container generates `config.json` at startup using environment variables (`API_URL`, `PRODUCTION`).

The container startup script is located at `scripts/entrypoint.sh`.

## Useful Scripts

```bash
# Development server
npm run start

# Production build
npm run build

# Development build with watch
npm run watch

# Unit tests
npm run test

# CI-style tests
npm run test:ci

# Performance testing
npm run lighthouse              # Run all Lighthouse tests
npm run lighthouse:collect     # Collect Lighthouse data
npm run lighthouse:assert      # Assert performance thresholds
./run-lighthouse-tests.sh      # Full performance test suite

# Formatting
npm run format
```

## Testing

The frontend uses Jasmine and Karma for unit testing, with Angular's testing utilities for component and service validation.

### Test Types

**Unit Tests** (`*.spec.ts` files):
- Component logic and event handling
- Service method behavior
- Pipe transformations
- Route guard logic

**Component Tests**:
- Template binding validation
- Input/output property testing
- Lifecycle hook behavior
- Dependency injection verification

**Performance Tests** (Lighthouse):
- Core Web Vitals (FCP, LCP, CLS, TBT)
- Performance scoring
- Accessibility auditing
- SEO optimization checks

### Running Tests

```bash
# Single run with coverage (CI mode)
npm run test:ci

# Watch mode for development
npm run test

# Run specific test file
ng test --include='**/header.component.spec.ts'

# Run with code coverage
ng test --code-coverage

# Headless browser (CI environments)
CHROME_BIN=$(which chromium-browser) npm run test:ci
```

### Performance Testing with Lighthouse

Lighthouse audits measure performance, accessibility, best practices, and SEO metrics.

```bash
# Build frontend and run Lighthouse
./run-lighthouse-tests.sh

# Or run manually:
npm run build
npm run serve:ssr:tfm_front &
# Wait for server to start
npx lhci autorun
```

**Lighthouse Metrics Tracked**:
- Performance score (target: >70)
- First Contentful Paint (FCP): <3s
- Largest Contentful Paint (LCP): <4s
- Total Blocking Time (TBT): <500ms
- Cumulative Layout Shift (CLS): <0.1
- Accessibility score (target: >80)
- Best Practices score (target: >80)
- SEO score (target: >80)

**Configuration**: See `lighthouserc.json` for test URLs and thresholds.

**Reports**: Generated in `lighthouse-reports/` directory.

### Writing New Tests

**For components**:
```typescript
describe('ComponentNameComponent', () => {
  let component: ComponentNameComponent;
  let fixture: ComponentFixture<ComponentNameComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComponentNameComponent],
      // Add providers, mock services
    }).compileComponents();

    fixture = TestBed.createComponent(ComponentNameComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should handle input change', () => {
    component.inputProperty = 'test';
    fixture.detectChanges();
    expect(component.output).toEqual('expected');
  });
});
```

**For services**:
```typescript
describe('ServiceNameService', () => {
  let service: ServiceNameService;
  let httpClient: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ServiceNameService],
      imports: [HttpClientTestingModule]
    });

    service = TestBed.inject(ServiceNameService);
    httpClient = TestBed.inject(HttpTestingController);
  });

  it('should make HTTP call', () => {
    service.getData().subscribe(data => {
      expect(data).toEqual(expectedData);
    });

    const req = httpClient.expectOne('/api/data');
    req.flush(expectedData);
  });
});
```

### Test Configuration

**Karma Config** (`karma.conf.js`):
- Chrome/Chromium browser launcher
- Jasmine framework
- Angular CLI builder
- Coverage reporters (lcov, text)

**Angular Test Config** (`angular.json`):
- Test builder: `@angular-devkit/build-angular:karma`
- TypeScript compilation for tests
- Source maps for debugging

### Current Coverage

The frontend includes 69 tests covering:
- Core authentication flows
- Shared components (header, footer)
- Data services
- Route guards
- Utility functions and pipes

### Troubleshooting Tests

**Common issues**:
- **Browser not found**: Set `CHROME_BIN` environment variable
- **Memory issues**: Use `--no-watch` flag or increase Node memory
- **Flaky tests**: Ensure proper async handling with `async/await`
- **Import errors**: Verify all dependencies are installed with `npm ci`

## Main Structure

```text
src/app/
  core/         # Authentication, interceptors, global services
  data/         # Interfaces and HTTP services
  features/     # Feature/page components
  components/   # Shared UI components
  layout/       # Base application layout
  shared/       # Constants and reusable utilities
```

## Frontend Architecture

Flow summary:

1. Router loads the active feature view.
2. Feature components call `data/services` for API interactions.
3. Auth interceptor adds JWT tokens to outgoing HTTP requests.
4. API responses are rendered by the component layer.

See full system architecture in [`../docs/architecture.md`](../docs/architecture.md).

## Troubleshooting

- `EADDRINUSE: address already in use`
  - The port is already occupied. Change the port or stop the existing process.
- CORS errors or failing API requests
  - Verify that backend is running on the configured API URL (`http://localhost:8080/api`).
- SSR does not start after a successful build
  - Run `npm run build` first, then `npm run serve:ssr:tfm_front`.
