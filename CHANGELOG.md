# Changelog

All notable changes to this project will be documented in this file.

## 1.0.0 - 2026-02-23
- Production-ready backend config with required secrets and env-driven runtime.
- Hash-chained audit logging for login, pairing, recipes, jobs, and telemetry checkpoints.
- Evidence package export (zip) with manifest hashes and telemetry samples.
- Recipes CRUD + print job lifecycle endpoints and dashboard wiring.
- gRPC telemetry stream verified end-to-end with desktop + Android clients.
- Simulator improvements and expanded test coverage (audit integrity + gRPC stream).
- Enforced CI quality gates (`ktlint`, `detekt`, lint, tests) with strict mode and warnings-as-errors.
- Added shared-network smoke tests for cleartext/TLS safeguards and auth token persistence.
- Hardened desktop token storage at rest and switched clients to TLS-first defaults.
- Added release and security documentation for reproducible v1.0.0 tagging and distribution.

## 1.0.0 - 2026-02-24
- Monorepo reconfiguration with `apps/`, `services/`, `agents/`, `shared/`, and `ops` layout.
- RegenOps Control KMP app (Android/Desktop) with protocol list, run control, and live telemetry streaming.
- gRPC services for Protocol/Run/Gateway/Metrics + HTTP fallback endpoints for Control.
- Device Gateway agent with register/heartbeat + simulated telemetry/events.
- Observability baseline: JSON logs, `/metrics` Prometheus, OTEL exporter hook.
- DevEx: docker-compose, Makefile, and Windows runLocal script.

## 1.0.0 - 2026-03-02
- Device Support Tiers v1.0.0 with shared domain policy, HTTP + gRPC policy endpoints, and client/device header propagation.
- Server-enforced capability checks with default-deny for unmapped mutating endpoints.
- Audited device-tier allow/deny decisions and added capability decision metrics.
- Tier-aware UI gating in Control KMP (Android/Desktop) with capability-based navigation.
- Device gateway identifies as `EMBEDDED_TOUCHSCREEN` Tier1 with policy headers.
- Added tier enforcement tests (HTTP + gRPC) and device-policy contract tests.
- Updated runbook and pilot install checklist for Phase 4 deployment.
