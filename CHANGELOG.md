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
