# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Standardized release tracking using `CHANGELOG.md` following Keep a Changelog 1.1.0.
- Central roadmap document (`ROADMAP.md`) categorizing tasks in progress, short-term, medium-term, and long-term milestones.
- In-app notification center and multi-channel preferences (`APP`, `EMAIL`, `BOTH`, `NONE`) with automatic match scheduling alerts, interactive header bell popover, unread counter, and account settings preference controls.
- Club organizations with customizable public pages (branding, description, contact/social links, visibility), member management with `OWNER`/`ADMIN`/`COACH`/`MEMBER` roles, granular permissions, and public internal rankings.
- Optional club association for tournaments, including club page tournament listings and `MANAGE_TOURNAMENTS` permission checks.
- Excel (`.xlsx`) tournament export with dedicated sheets for tournament information, registered players, matches and results, pending-match placeholders, and courts.
- Full-stack observability integration with OpenTelemetry distributed tracing (`micrometer-tracing-bridge-otel` + OTLP exporter to Tempo), log correlation (`traceId`/`spanId`), Prometheus metrics, Loki log aggregation, and provisioned Grafana dashboards (`compose.observability.yaml`).
- Installable Progressive Web App with web manifest, generated icons, app-shell service worker and cacheable public routes.
- Read-only offline mode: service worker data groups for public tournaments, clubs, rankings and reference data, plus IndexedDB snapshots of tournament and inscription details with a visible cache timestamp.
- Connectivity and update UX: global offline/reconnection banner and an explicit "Actualizar ahora" prompt driven by `SwUpdate`.
- Web Push notifications with VAPID: per-device subscriptions stored in `push_subscriptions`, automatic delivery when the notification channel is `APP` or `BOTH`, cleanup of expired endpoints (HTTP 404/410), and a test notification from the account settings screen.
- Account settings push controls with browser support detection, permission errors and an iOS hint explaining that push only works after adding the PWA to the home screen (iOS 16.4+).

### Changed
- Browser API calls now use a same-origin `/api` base URL through a development proxy, while SSR keeps an absolute backend URL (`PUBLIC_API_URL` for the client, `API_URL` for the server).
- Runtime configuration is cached in `localStorage` so the app keeps its API URL when started offline.

### Security
- Authenticated endpoints (`/api/auth/**`, personal calendar routes) are excluded from service worker caching; only public read endpoints are stored.
- The Server-Sent Events stream bypasses the service worker to avoid caching a never-ending response.

### Documentation
- Added Architecture Decision Records under `docs/decisions/` covering PWA-first strategy with a future Capacitor path, read-only offline design, same-origin API configuration and connectivity/update UX.
- Documented Web Push architecture, VAPID key generation (`scripts/generate-vapid-keys.mjs`) and the notification/push API surface.

## [0.1.0] - 2026-10-29

### Added
- Hexagonal architecture backend in Spring Boot 4.0 / Java 25 with PostgreSQL persistence and Flyway migrations.
- Complete tournament lifecycle management (Draft, Open, Closed, In Progress, Completed, Cancelled).
- Event categories, courts management, scheduling, conflict detection, and match scheduling swap capabilities.
- Automated draw, stage, and match generation strategies (single elimination, consolation, round robin).
- Real-time tournament updates using Server-Sent Events (SSE).
- Authentication and authorization with JWT tokens, refresh tokens, role checks, and email confirmation flows.
- Comprehensive member profiles, player invitations, and participant registration.
- Professional player ranking query services and CSV batch import integration.
- Public legal document versioning and privacy consent tracking.
- Angular 20 zoneless frontend with SSR, TailwindCSS, and responsive tournament brackets/draws/calendar views.
- PDF bracket export service with client-side rendering.
- Docker Compose local development and EC2 / Nginx deployment scripts with Let's Encrypt SSL support.
