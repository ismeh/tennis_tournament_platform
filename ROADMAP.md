# Project Roadmap

Roadmap of planned improvements, architectural refinements, and feature additions for the Tennis Tournament Platform.

Tasks follow a clear progression lifecycle:
1. **In Progress**: Actively being designed or implemented.
2. **Short Term**: Immediate next steps, bug fixes, test coverage, and refined developer UX.
3. **Medium Term**: Substantial functional additions and operational capabilities.
4. **Long Term**: Strategic features, scalability improvements, integrations, and ecosystem maturity.

> [!NOTE]
> Once a feature is completed and verified, it is moved from this roadmap into [`CHANGELOG.md`](./CHANGELOG.md) in a concise, human-readable summary under `[Unreleased]` (or the corresponding version release).

---

## 🚧 In Progress

- [ ] **Standardized Versioning & Documentation**: Establish Keep a Changelog 1.1.0 and centralized roadmap tracking across `CHANGELOG.md`, `ROADMAP.md`, `AGENTS.md`, and `README.md`.
- [ ] **Branch Synchronization**: Consolidate develop/main changes into `experimental` and maintain clean integration across working branches.

---

## 🎯 Short Term (Next Milestones)

- [ ] **CI/CD Pipeline Robustness**:
  - Unify GitHub Actions workflows for automated verification on PRs (`mvn test`, `npm run test:ci`, `npm run build`).
  - Add branch protection rules and automated badge reporting.
- [ ] **Frontend Testing & Bundle Budgets**:
  - Adjust Angular initial bundle budget warnings in `angular.json` for lazy-loaded assets.
  - Expand component unit tests for edge cases in match details and modal dialogues.
- [ ] **Environment Configuration & Setup Ergonomics**:
  - Streamline `.env` setup scripts for one-click local startup with Docker.
  - Improve Swagger/OpenAPI UI integration for interactive backend endpoint testing.

---

## 🚀 Medium Term

- [ ] **Comprehensive Tournament Formats**:
  - Complete support for double-elimination and round-robin draw/stage generation with complete UI brackets.
  - Automated tiebreak and bye resolution logic visualization in bracket component.
- [ ] **Umpire & Live Match Scoring**:
  - Live umpire interface for point-by-point / game-by-game scoring updates in real time.
  - Enhanced SSE listener handling on client with optimistic UI updates.
- [ ] **Notification Center**:
  - Email and in-app notifications for match schedule updates, court reassignments, and tournament invitations.
- [ ] **Comprehensive Exporting**:
  - Excel/CSV export of inscriptions, draw results, and match scores.

---

## 🔮 Long Term

- [ ] **Mobile Application / PWA**:
  - Progressive Web App (PWA) optimization and offline match viewing capabilities for players on court.
- [ ] **External Federation Integrations**:
  - Direct synchronization with national federation APIs (RFET / ITF / ATP / WTA) for real-time player licenses and points.
- [ ] **Multi-Club & Multi-Tenant Support**:
  - Granular organizational tenants for multi-club federations and venue networks.
- [ ] **Advanced Observability & Scalability**:
  - OpenTelemetry distributed tracing and Grafana dashboard templates for high-concurrency tournament traffic.
