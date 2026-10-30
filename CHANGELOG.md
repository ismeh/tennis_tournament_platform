# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Standardized release tracking using `CHANGELOG.md` following Keep a Changelog 1.1.0.
- Central roadmap document (`ROADMAP.md`) categorizing tasks in progress, short-term, medium-term, and long-term milestones.
- In-app notification center and multi-channel preferences (`APP`, `EMAIL`, `BOTH`, `NONE`) with automatic match scheduling alerts, interactive header bell popover, unread counter, and account settings preference controls.

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
