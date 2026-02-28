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

# ExecPlan: RegenOps Monorepo Reconfiguration (2026-02-24)

## Milestones
1) Repository audit + incremental migration plan with risks.
2) Monorepo layout + Gradle settings for `apps/`, `services/`, `agents/`, `shared/`, `ops/`.
3) Protobuf contracts + stub generation for server, gateway, and KMP client (HTTP fallback strategy).
4) Walking skeleton: core-server + Postgres + gateway sim + control-kmp UI with streaming.
5) Observability baseline (JSON logs, Prometheus metrics, OTEL tracing) + DevEx + docs.

## File-by-File Change List
- `PLANS.md`: Track migration plan, risks, and progress.
- `settings.gradle.kts`: New module includes for monorepo layout.
- `build.gradle.kts`: Shared configuration for new modules + reproducible builds.
- `gradle/libs.versions.toml`: Add gRPC, OTEL, Prometheus, SQLDelight, Koin, protobuf tooling.
- `shared/proto/*`: Protobuf contracts for Protocol/Run/Gateway/Metrics services.
- `shared/domain/*`: Shared domain models and API contracts for KMP.
- `services/core-server/*`: Ktor + gRPC modular monolith, Postgres persistence.
- `agents/device-gateway/*`: JVM agent with gRPC client + simulated telemetry.
- `apps/control-kmp/*`: KMP app (Android/Desktop) with protocol list + run control + live streaming.
- `ops/docker-compose.yml`: Core-server + Postgres + gateway run.
- `ops/Makefile`, `ops/runLocal.ps1`: runLocal/test/lint/format helpers.
- `docs/*`: Local run + gateway deployment + ops notes.
- `README.md`: Top-level instructions for running the monorepo.

## Verification Commands
- `./gradlew.bat :services:core-server:test`
- `./gradlew.bat :agents:device-gateway:test`
- `./gradlew.bat :apps:control-kmp:androidApp:assembleDebug`
- `./gradlew.bat :apps:control-kmp:desktopApp:run`
- `docker-compose -f ops/docker-compose.yml up --build`

## Rollback Plan
- Revert files listed above.
- Remove new modules from `settings.gradle.kts`.
- Re-run baseline build tasks to confirm stability.

## Progress
- [x] Repository audit + incremental migration plan with risks.
- [x] Monorepo layout + Gradle settings.
- [x] Protobuf contracts + stub generation.
- [x] Walking skeleton end-to-end.
- [x] Observability + DevEx + docs.

---

# ExecPlan: Fix Test Build (Proto Collision) (2026-02-24)

## Milestones
1) Remove regenops proto collision from local proto module and consume server contracts artifact.
2) Update server/gateway dependencies to use contracts artifact.
3) Align JVM toolchain to contracts compatibility.
4) Update gateway code to contracts package/API changes.
5) Align core-server code with contracts package/API changes + missing observability deps.
6) Re-run `./gradlew.bat test` and address any remaining failures.

## File-by-File Change List
- `PLANS.md`: Track test-fix plan.
- `build.gradle.kts`: Ensure `mavenLocal()` is available for contracts resolution.
- `shared/proto/src/main/proto/regenops/regenops.proto`: Remove local RegenOps proto (use contracts artifact).
- `services/core-server/build.gradle.kts`: Add contracts artifact dependency.
- `agents/device-gateway/build.gradle.kts`: Add contracts artifact dependency.
- `services/core-server/build.gradle.kts`: Align JVM toolchain with contracts artifact.
- `agents/device-gateway/build.gradle.kts`: Align JVM toolchain with contracts artifact.
- `agents/device-gateway/src/main/kotlin/com/neogenesis/platform/gateway/DeviceGateway.kt`: Align imports and message fields to contracts package.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/RegenOpsServices.kt`: Align gRPC service stubs to contracts package/API.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/RegenOpsStore.kt`: Align in-memory store types to contracts package/API.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/Main.kt`: Fix call logging MDC usage.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/RegenOpsHttpModule.kt`: Align HTTP start run DTO with contracts.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/observability/OpenTelemetryConfig.kt`: Use AttributeKey for service name resource attributes.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/observability/Prometheus.kt`: Update micrometer prometheus import package.
- `shared/network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/ApiCall.kt`: Expose correlation id helper for inline API calls.
- `services/core-server/src/test/kotlin/com/neogenesis/platform/core/ProtocolServiceTest.kt`: Update imports for contracts package.
- `services/core-server/src/test/kotlin/com/neogenesis/platform/core/RunServiceTest.kt`: Update test requests for contracts package.
- `gradle/libs.versions.toml`: Align protobuf version with contracts artifact.
- `apps/control-kmp/shared/build.gradle.kts`: Align Android JVM target with Java compile options.
- `apps/control-kmp/shared/src/commonMain/kotlin/Boolean.kt`: Provide SQLDelight Boolean alias.
- `apps/control-kmp/shared/src/androidMain/kotlin/com/neogenesis/platform/control/data/remote/GrpcControlApi.kt`: Align gRPC client to contracts package.
- `apps/control-kmp/shared/src/androidMain/kotlin/com/neogenesis/platform/control/data/stream/GrpcRegenOpsStreamClient.kt`: Align stream client to contracts events/telemetry.
- `apps/control-kmp/shared/src/androidMain/kotlin/com/neogenesis/platform/control/platform/AndroidPlatformModule.kt`: Update Koin bindings for platform logger.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/local/RegenOpsLocalDataSource.kt`: Fix SQLDelight query parameter naming.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/oidc/OidcModels.kt`: Expose OIDC models for public API.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/oidc/OidcDeviceAuthService.kt`: Fix form post handling and error logging.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/oidc/OidcRepository.kt`: Align HTTP error handling.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/CommercialApi.kt`: Add CommercialApi interface and correlation id helper.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/HttpControlApi.kt`: Align HTTP control API to contracts.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/di/KoinModules.kt`: Fix Koin init return type and SQLDelight adapters.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt`: Align HTTP error handling.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`: Fix composable control flow and formatting.

## Verification Commands
- `./gradlew.bat test`

## Rollback Plan
- Revert the files listed above.
- Re-run `./gradlew.bat test`.

## Progress
- [x] Remove local regenops proto collision.
- [x] Add contracts artifact dependencies.
- [x] Align JVM toolchain to contracts compatibility.
- [x] Update gateway code to contracts package/API changes.
- [x] Align core-server code with contracts package/API changes + missing observability deps.
- [x] Re-run tests.

---

# ExecPlan: RegenOps Control KMP Migration (2026-02-24)

## Milestones
1) Create `apps/control-kmp` module structure with shared/data/presentation layers and platform apps.
2) Implement SQLDelight cache + repositories and wire Koin DI across Android/Desktop.
3) Add OIDC device auth flow + secure token storage (Android encrypted prefs, Desktop keystore file).
4) Implement gRPC streaming with reconnection/backoff + correlation_id propagation; keep HTTP/JSON fallback wiring.
5) Deliver core screens + navigation and update README + env config documentation.

## File-by-File Change List
- `PLANS.md`: Track ExecPlan progress for RegenOps Control KMP migration.
- `settings.gradle.kts`: Include `apps/control-kmp` modules.
- `gradle/libs.versions.toml`: Add SQLDelight + Koin + AppAuth versions (if needed).
- `apps/control-kmp/shared/build.gradle.kts`: KMP shared module with SQLDelight + Koin + Compose Multiplatform.
- `apps/control-kmp/shared/src/commonMain/sqldelight/.../RegenOpsDatabase.sq`: Protocols + runs cache schema + queries.
- `apps/control-kmp/shared/src/commonMain/kotlin/...`: Domain/use cases, data repositories, OIDC device auth flow, shared UI.
- `apps/control-kmp/shared/src/androidMain/kotlin/...`: Android SQLDelight driver + Android token storage + gRPC stream client.
- `apps/control-kmp/shared/src/jvmMain/kotlin/...`: Desktop SQLDelight driver + keystore token storage + gRPC stream client.
- `apps/control-kmp/androidApp/build.gradle.kts`: Android app wiring + BuildConfig env config.
- `apps/control-kmp/androidApp/src/main/AndroidManifest.xml`: App manifest + cleartext policy (debug) + launcher.
- `apps/control-kmp/androidApp/src/main/java/.../MainActivity.kt`: Compose entrypoint.
- `apps/control-kmp/desktopApp/build.gradle.kts`: Desktop app wiring.
- `apps/control-kmp/desktopApp/src/jvmMain/kotlin/.../DesktopApp.kt`: Compose entrypoint.
- `.env.example`: Add RegenOps Control OIDC + gRPC/HTTP env hints.
- `README.md`: Add run steps for `apps/control-kmp` Android/Desktop.

## Verification Commands
- `./gradlew.bat :apps:control-kmp:androidApp:assembleDebug`
- `./gradlew.bat :apps:control-kmp:desktopApp:run`
- `./gradlew.bat :apps:control-kmp:shared:compileKotlinMetadata`

## Rollback Plan
- Revert files listed above.
- Remove `apps/control-kmp` modules from `settings.gradle.kts`.
- Re-run `./gradlew.bat :apps:control-kmp:androidApp:assembleDebug` to confirm baseline restoration.

## Progress
- [x] Module structure + Gradle wiring created.
- [x] SQLDelight cache + repositories + Koin DI implemented.
- [x] OIDC device auth flow + secure token storage implemented.
- [x] gRPC streaming reconnection/backoff + correlation ID propagation implemented.
- [x] UI + navigation + README/env updates completed.

---

# ExecPlan: Control App Exports UI (2026-02-24)

## Milestones
1) Add Exports screen gated by `founder_mode`.
2) Wire minimal Exports API client (HTTP) with loading/success/error states.
3) Add Android/Desktop share/save handlers and README note.

## File-by-File Change List
- `PLANS.md`: Track Exports UI patch.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt`: Add Exports screen + state.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt`: Export actions + founder flag.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt`: Route to Exports screen.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`: Exports UI composable.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/ExportsApi.kt`: HTTP export client.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/di/KoinModules.kt`: Bind ExportsApi.
- `apps/control-kmp/androidApp/src/main/java/com/neogenesis/platform/control/android/MainActivity.kt`: Share exports on Android.
- `apps/control-kmp/desktopApp/src/jvmMain/kotlin/com/neogenesis/platform/control/desktop/DesktopApp.kt`: Save/open exports on Desktop.
- `README.md`: Document `founder_mode` env flag and exports.

## Verification Commands
- `./gradlew.bat :apps:control-kmp:androidApp:assembleDebug`

## Rollback Plan
- Revert the files listed above.
- Re-run the verification command.

## Progress
- [x] Add Exports UI and wiring.

---

# ExecPlan: Control App Trace UI (2026-02-24)

## Milestones
1) Add Trace screen gated by `FOUNDER_MODE`.
2) Wire minimal Trace API client (HTTP + demo) with loading/success/error states.
3) Add navigation entry for Trace screen.

## File-by-File Change List
- `PLANS.md`: Track Trace UI patch.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt`: Add Trace screen + state.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt`: Trace actions + founder flag gating.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt`: Route to Trace screen.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`: Trace UI composable.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/TraceApi.kt`: HTTP trace client.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/di/KoinModules.kt`: Bind TraceApi.

## Verification Commands
- `./gradlew.bat :apps:control-kmp:androidApp:assembleDebug`

## Rollback Plan
- Revert the files listed above.
- Re-run the verification command.

## Progress
- [x] Add Trace UI and wiring.

---

# ExecPlan: Control App Demo Mode (2026-02-24)

## Milestones
1) Add simulated run toggle + banner (founder gated).
2) Wire ViewModel state + Run Control UI.

## File-by-File Change List
- `PLANS.md`: Track Demo Mode patch.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt`: Add simulated run state.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt`: Toggle + banner state.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt`: Banner rendering.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`: Run Control toggle UI.

## Verification Commands
- `./gradlew.bat :apps:control-kmp:androidApp:assembleDebug`

## Rollback Plan
- Revert the files listed above.

## Progress
- [x] Add simulated run toggle + banner.

---

# ExecPlan: RegenOps Control App Reconfiguration (2026-02-24)

## Milestones
1) Confirm current Android app architecture + networking/logging hooks and define RegenOps screen flow + data contracts.
2) Add shared-network models/APIs + correlation ID propagation + structured logging metadata.
3) Add Android offline cache for protocols + last runs and wire repository to UI.
4) Implement RegenOps Control screens + navigation and feature-flag gating while preserving auth/billing/observability flows.
5) Add streaming stub + instrumentation test (“StartRun -> see first event”) and verify build targets.

## File-by-File Change List
- `PLANS.md`: Track ExecPlan progress for RegenOps Control.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/HttpClientFactory.kt`: Add correlation ID header propagation and logging metadata hook.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/AppLogger.kt`: Extend logger metadata usage for structured logs (if needed).
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/ProtocolsApi.kt`: New API contract + Ktor implementation for protocols/versions.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/RunsApi.kt`: New API contract + Ktor implementation for runs/events.
- `shared-network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/RegenOpsModels.kt`: Protocol/run/event/telemetry models.
- `androidApp/build.gradle.kts`: Add Room + test dependencies (if required).
- `androidApp/src/main/java/com/neogenesis/platform/android/data/RegenOpsDatabase.kt`: Room database for protocols + last runs cache.
- `androidApp/src/main/java/com/neogenesis/platform/android/data/RegenOpsDao.kt`: DAO for protocols/runs cache.
- `androidApp/src/main/java/com/neogenesis/platform/android/data/RegenOpsRepository.kt`: Repository bridging network + cache.
- `androidApp/src/main/java/com/neogenesis/platform/android/ui/RootScreen.kt`: Replace dashboard with RegenOps Control screens + navigation and feature flag gating.
- `androidApp/src/main/java/com/neogenesis/platform/android/ui/RegenOpsScreens.kt`: Protocols list/detail, run control, live run UI components.
- `androidApp/src/main/java/com/neogenesis/platform/android/ui/TelemetryChart.kt`: Basic telemetry chart placeholder.
- `androidApp/src/androidTest/java/com/neogenesis/platform/android/RegenOpsRunInstrumentationTest.kt`: “StartRun -> see first event” test.

## Verification Commands
- `./gradlew.bat :androidApp:assembleDebug`
- `./gradlew.bat :shared-network:test`
- `./gradlew.bat :domain:test`

## Rollback Plan
- Revert updated files listed above.
- Remove new Room database files and dependencies.
- Re-run `./gradlew.bat :androidApp:assembleDebug` to confirm baseline stability.

## Progress
- [x] Added RegenOps models, APIs, and correlation ID propagation + logging hooks.
- [x] Implemented Android Room cache for protocols + runs with repository integration.
- [x] Rebuilt Android UI into RegenOps Control screens with stub backend flow.
- [x] Added gRPC stream client + stub stream client wiring for live telemetry/events.
- [x] Added instrumentation test for “StartRun -> see first event”.

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

# ExecPlan: Ktlint Generated Sources Exclusion (2026-02-25)

## Milestones
1) Inspect ktlint task wiring for generated sources.
2) Apply robust excludes for generated/build outputs to ktlint tasks.
3) Confirm CI-targeted modules no longer scan generated gRPC code.

## File-by-File Change List
- `PLANS.md`: Add this execution plan and keep progress current.
- `build.gradle.kts`: Ensure ktlint tasks exclude `build/` + generated source trees across modules.

## Verification Commands
- `./gradlew.bat :shared:data:ktlintCommonMainSourceSetCheck`
- `./gradlew.bat :agents:device-gateway:ktlintMainSourceSetCheck`

## Rollback Plan
- Revert `build.gradle.kts`.
- Re-run the two ktlint commands above to confirm prior behavior.

## Progress
- [ ] Identify why generated sources are being scanned on CI.
- [x] Apply ktlint task exclusions for generated/build sources.
- [ ] Validate ktlint tasks no longer report generated gRPC files.

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
