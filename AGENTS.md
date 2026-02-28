# AGENTS

## ExecPlan
For any multi-step change, create or update `PLANS.md` with milestones, a file-by-file change list, verification commands, and a rollback plan before implementation. Keep it current as work progresses.

## Verification Commands
- `./gradlew.bat test`
- `./gradlew.bat :backend:test`
- `./gradlew.bat :domain:test`
- `./gradlew.bat :shared-network:test`
- `./gradlew.bat :desktopApp:run`
- `./gradlew.bat :androidApp:assembleDebug`

## Release Checklist
- Version bumped to release across Gradle modules and `VERSION`.
- `CHANGELOG.md` updated with user-visible release notes.
- `.env.example` updated and secrets required at runtime.
- Dockerfile(s) + `docker-compose.yml` verified for backend + Postgres.
- Evidence package export documented with example output.
- Audit chain integrity tests + integration tests passing.
- gRPC telemetry stream verified end-to-end.
- README updated with quickstart, env vars, run, tests, evidence export.
