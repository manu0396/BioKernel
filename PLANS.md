# ExecPlan v1.0.0

## Milestones
1) Baseline discovery + release scaffolding
2) Backend hardening: audit chain, evidence package export, recipes/jobs, telemetry durability
3) Client experience: auth/session persistence, telemetry stream, recipes/jobs UX
4) Simulator + documentation updates
5) Verification + release polish

## File-by-File Change List
- `AGENTS.md`: Add ExecPlan/verification/release checklist guidance.
- `PLANS.md`: Define milestones, change list, verification commands, rollback.
- `backend/src/main/kotlin/com/neogenesis/platform/backend/Main.kt`: Wire new services, audit logging, evidence export.
- `backend/src/main/kotlin/com/neogenesis/platform/backend/modules/AuthModule.kt`: Audit login/register events.
- `backend/src/main/kotlin/com/neogenesis/platform/backend/modules/DeviceModule.kt`: Audit pairing events.
- `backend/src/main/kotlin/com/neogenesis/platform/backend/modules/PrintJobModule.kt`: Job lifecycle endpoints + audit logs.
- `backend/src/main/kotlin/com/neogenesis/platform/backend/modules/EvidenceModule.kt`: Evidence package export endpoint.
- `backend/src/main/kotlin/com/neogenesis/platform/backend/modules/TelemetryModule.kt`: Telemetry checkpoint audit + export fixes.
- `backend/src/main/kotlin/com/neogenesis/platform/backend/storage/Repositories.kt`: Fix telemetry serialization, add recipe repository.
- `backend/src/main/kotlin/com/neogenesis/platform/backend/storage/Tables.kt`: Add recipes tables.
- `backend/src/main/resources/db/migration/V2__recipes.sql`: New recipe schema.
- `domain/src/commonMain/kotlin/com/neogenesis/platform/shared/evidence/EvidenceLogger.kt`: Deterministic chain creation + validation improvements.
- `domain/src/commonTest/kotlin/com/neogenesis/platform/shared/evidence/EvidenceChainTest.kt`: Add tamper detection test.
- `backend/src/test/kotlin/com/neogenesis/platform/backend/EvidenceExportTest.kt`: Add integration test for evidence export.
- `backend/src/test/kotlin/com/neogenesis/platform/backend/GrpcTelemetryStreamTest.kt`: Add gRPC telemetry stream test.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/TokenStorage.kt`: Add persistent storage contracts.
- `androidApp/src/main/java/com/neogenesis/platform/android/...`: Add auth/session UI + persistent token storage.
- `desktopApp/src/main/kotlin/com/neogenesis/platform/desktop/...`: Add auth/session UI + gRPC telemetry stream.
- `tools/device_simulator.py`: Add rate config docs + CLI help.
- `docs/RUNBOOK.md`: Add evidence export + verification steps.
- `README.md`: Update quickstart, env vars, run commands, evidence export.
- `CHANGELOG.md`: Add 1.0.0 release notes.
- `.env.example`: Ensure required envs + defaults documented.

## Verification Commands
- `./gradlew.bat test`
- `./gradlew.bat :backend:test`
- `./gradlew.bat :domain:test`
- `./gradlew.bat :desktopApp:run`
- `./gradlew.bat :androidApp:assembleDebug`
- `docker-compose up --build`

## Rollback Plan
- Revert changes with `git restore` on touched files.
- If migrations applied locally, drop and recreate the dev database container volume.
- Re-run tests to confirm baseline stability.

---

# ExecPlan: Quality Noise Cleanup (2026-02-23)

## Milestones
1) Audit current quality plugin wiring (root + modules).
2) Apply global exclusions for generated/build outputs in ktlint/detekt.
3) Apply auto-formatting where safe.
4) Re-run quality/build checks and report residual real issues.

## File-by-File Change List
- `build.gradle.kts`: Add deterministic ktlint/detekt exclusions for generated/build outputs and non-source artifacts.
- `gradle.properties`: Align Java toolchain discovery behavior to stable Windows/PowerShell usage.

## Verification Commands
- `./gradlew.bat javaToolchains`
- `./gradlew.bat ktlintFormat`
- `./gradlew.bat ktlintCheck`
- `./gradlew.bat detekt`
- `./gradlew.bat clean build`

## Rollback Plan
- Revert `build.gradle.kts` and `gradle.properties`.
- Re-run `./gradlew.bat clean build` to confirm previous state.

---

# ExecPlan: Productization Monetization UX + Entitlements (App v1.0.0)

## Milestones
1) Discovery + planning artifacts.
2) Shared billing client and entitlement state foundation.
3) Upgrade/Manage UI flow integration on Android + Desktop.
4) Premium feature gating with paywall UX.
5) Tests, docs, and full verification.

## File-by-File Change List
- `PLANS.md`: Add this execution plan and keep progress current.
- `docs/PRODUCTIZATION_PLAN_APP_1.0.0.md`: Dedicated implementation/commit sequence for monetization.
- `data/src/commonMain/kotlin/com/neogenesis/platform/data/api/BillingDtos.kt`: Billing request/response DTOs.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/BillingApi.kt`: Billing API interface + Ktor implementation.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/EntitlementsRepository.kt`: TTL cache, offline grace logic, and reactive state.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/BillingModels.kt`: Feature flag enum and entitlement models.
- `shared-network/src/commonTest/kotlin/com/neogenesis/platform/shared/network/EntitlementsRepositoryTest.kt`: Unit tests with fake BillingApi.
- `androidApp/src/main/java/com/neogenesis/platform/android/ui/RootScreen.kt`: Upgrade screen, Subscribe/Manage flows, browser launch, paywall gating.
- `androidApp/src/main/java/com/neogenesis/platform/android/MainActivity.kt` (if needed): Lifecycle hooks for entitlement refresh.
- `desktopApp/src/main/kotlin/com/neogenesis/platform/desktop/ui/RootScreen.kt`: Upgrade screen, Subscribe/Manage flows, browser launch, paywall gating.
- `docs/BILLING_CLIENT.md`: Billing flow, entitlement refresh behavior, local testing strategy.
- `docs/RELEASING.md`: Release validation steps for paywall and entitlement refresh.

## Verification Commands
- `./gradlew clean build`
- `./gradlew.bat test`
- `./gradlew.bat :backend:test`
- `./gradlew.bat :domain:test`
- `./gradlew.bat :shared-network:test`
- `./gradlew.bat :desktopApp:run`
- `./gradlew.bat :androidApp:assembleDebug`

## Rollback Plan
- Revert the monetization-related files listed above.
- Re-run `./gradlew clean build` to verify baseline restored.
- If UI regressions persist, disable premium gates by defaulting to permissive branch while investigating.

## Progress
- [x] Planning artifacts created (`PLANS.md`, `docs/PRODUCTIZATION_PLAN_APP_1.0.0.md`).
- [x] Billing API + models + entitlement repository implemented.
- [x] Android/Desktop Upgrade UI and paywall gating integrated.
- [x] Shared-network unit tests for entitlement repository added.
- [x] Billing/release docs updated.
- [x] Full clean build green (`./gradlew clean build`) after telemetry stream race fix.
- [~] Manual UI verification of browser launch/paywall behavior - code wired; interactive confirmation pending local UI session.

## Follow-up: Stability + UI-State Test Layer
### Milestones
1) Stabilize flaky backend gRPC telemetry test behavior.
2) Add deterministic UI-state mapping layer for Upgrade/paywall states.
3) Add automated tests for UI-state mapping and gate decisions.
4) Re-run clean build + module checks.

### File-by-File Change List
- `backend/src/main/kotlin/com/neogenesis/platform/backend/grpc/PlatformGrpcServices.kt`: Ensure telemetry stream waits for initial request metadata before emitting frames.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/BillingUiState.kt`: Add pure mapping from `EntitlementsState` to renderable Upgrade state and paywall decision helper.
- `shared-network/src/commonTest/kotlin/com/neogenesis/platform/shared/network/BillingUiStateTest.kt`: Add unit tests for loading, unavailable, active, and locked states.
- `androidApp/src/main/java/com/neogenesis/platform/android/ui/RootScreen.kt`: Use shared UI-state mapper for Upgrade rendering and gate checks.
- `desktopApp/src/main/kotlin/com/neogenesis/platform/desktop/ui/RootScreen.kt`: Use shared UI-state mapper for Upgrade rendering and gate checks.

### Verification Commands
- `./gradlew clean build`
- `./gradlew.bat :backend:test`
- `./gradlew.bat :shared-network:test`
- `./gradlew.bat :androidApp:assembleDebug`
- `./gradlew.bat :desktopApp:compileKotlin`
