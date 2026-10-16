# Implementation Plan: RegenOps Control App MVP

**Branch**: `001-control-app-mvp` | **Date**: 2026-10-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-control-app-mvp/spec.md`

## Summary

Deliver a controlled-pilot v1.0.0 of the existing RegenOps Control app. First
confirm the identity, run-to-device, environment, and release paths; then
complete the Android and desktop operator journeys for authentication, protocol
selection, simulated and connected runs, telemetry monitoring, safe run
controls, run history, and report/evidence export. Reuse existing modules and
contracts unless the readiness audit demonstrates a concrete incompatibility.
Do not claim clinical or general-production readiness.

## Technical Context

**Language/Version**: Kotlin; Android/JVM toolchain settings are defined in the
existing Gradle modules (Android app targets JVM 17; shared KMP JVM toolchain 21).

**Primary Dependencies**: Existing Kotlin Multiplatform and Compose app,
Ktor-based core service, protobuf/gRPC contracts, PostgreSQL-backed service
configuration, and the existing device gateway. Do not introduce dependencies
without a requirement and compatibility review.

**Storage**: Existing core persistence for users, telemetry, and evidence;
existing app-local SQLDelight database. The MVP plan does not add a new store
unless the readiness audit proves one is necessary.

**Testing**: Root CI command `./gradlew test --no-daemon`; existing shared KMP
tests, core-server tests, Android UI/instrumentation tests, and desktop app tests.
Connected-device acceptance additionally requires an end-to-end pilot check.

**Target Platform**: Android and desktop Control app, core service, and one
representative supported physical device connected through the gateway.

**Project Type**: Kotlin Multiplatform application with Android and desktop
clients, a backend service, shared libraries, and a device gateway.

**Performance Goals**: No numeric latency or throughput target is established
for this pilot in the reviewed product documentation. Record the expected
telemetry cadence and define a freshness threshold during readiness discovery
before telemetry acceptance tests are finalized.

**Constraints**: Preserve API/protobuf compatibility; require authorization and
device-capability checks for protected actions; TLS-first deployment posture;
no credentials or sensitive patient/biometric data in source, fixtures, logs, or
demonstration materials; both simulated and physical-device run paths are in
scope. The pilot validates one named device/gateway configuration only.

**Scale/Scope**: One controlled pilot/demo release; Android and desktop; one
representative physical device/gateway for connected acceptance; the four user
journeys and 17 functional requirements in the specification.

## Constitution Check

**Pre-design gate: PASS**

- **Preserve Module Boundaries**: plan changes inside the existing app, shared,
  service, and gateway modules; no new project/module is planned.
- **Verify Behavior with Tests**: map feature requirements to focused tests and
  the root CI test command; include Android and desktop target checks.
- **Protect Secrets and Sensitive Data**: retain TLS and encrypted-token
  protections; pilot fixtures and demo data must not contain real sensitive
  data.
- **Maintain Compatibility and Data Integrity**: inventory existing HTTP/gRPC
  contracts and evidence behavior before proposing changes; use additive,
  backward-compatible changes where possible.
- **Keep Changes Reviewable and Documented**: deliver in independently
  reviewable journey slices; document limitations and pilot setup.

**Post-design gate: PASS WITH RELEASE GATES**. The proposed artifacts add no
architecture or dependency. Implementation and release remain gated on choosing
and documenting a supported physical device/gateway, validating the complete
run-to-telemetry path, resolving version/build-command drift, and defining
telemetry freshness against the selected device's expected cadence. No
constitution exception is currently required.

## Repository Structure

### Feature Documentation

```text
specs/001-control-app-mvp/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── readiness-matrix.md
├── contracts/
│   ├── control-http.md
│   └── device-streams.md
├── checklists/
│   └── requirements.md
└── tasks.md                 # Dependency-ordered implementation and acceptance tasks
```

### Existing Source and Test Structure

```text
apps/control-kmp/
├── shared/src/commonMain/kotlin/          # Shared Control state, UI, and data flows
├── shared/src/commonTest/kotlin/          # Shared behavior tests
├── androidApp/src/main/                   # Android application
├── androidApp/src/androidTest/            # Android UI/instrumentation tests
└── desktopApp/src/jvmMain/                # Desktop application

services/core-server/
├── src/main/kotlin/                       # HTTP/gRPC services and persistence
└── src/test/kotlin/                       # Service, security, stream, and export tests

agents/device-gateway/src/main/kotlin/     # Gateway process and device stream connection
shared/proto/src/main/proto/               # Existing protobuf contracts
```

**Structure Decision**: Use the existing Gradle modules and source sets listed
above. Add no new application module. Put feature-specific documentation under
`specs/001-control-app-mvp/`; put contract notes there as compatibility
documentation, not as a replacement for generated protobuf or service code.

## Candidate Implementation File Map

These are the first locations to inspect during `/speckit-tasks`; change only
where the Phase 0 gap matrix confirms work is needed.

| Concern | Existing locations to inspect | Existing verification |
|---|---|---|
| Shared operator screens and run state | `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt`, `RegenOpsViewModel.kt`, `Screens.kt`, `RegenOpsUiState.kt` | `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModelTest.kt` |
| OIDC and API clients | `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/oidc/`, `data/remote/`, `data/stream/` | Shared common/JVM tests; add focused client tests if a contract mismatch is found |
| Android and desktop integration | `apps/control-kmp/androidApp/src/main/`, `apps/control-kmp/desktopApp/src/jvmMain/` | `apps/control-kmp/androidApp/src/androidTest/`, desktop tests under the app module |
| HTTP run/auth/export behavior | `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/RegenOpsHttpModule.kt`, `AuthModule.kt`, `ExportsAliasModule.kt` | `services/core-server/src/test/kotlin/com/neogenesis/platform/core/` focused auth/run/export tests |
| gRPC/device handoff | `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/`, `agents/device-gateway/src/main/kotlin/com/neogenesis/platform/gateway/`, `shared/proto/src/main/proto/` | Core gRPC, mTLS, telemetry, and device-policy tests |
| Release and operator documentation | `README.md`, `CHANGELOG.md`, `VERSION`, root/app Gradle build files (including `apps/control-kmp/androidApp/build.gradle.kts`), `docs/PILOT_INSTALL_CHECKLIST.md`, `docs/RELEASING.md`, `ops/docker-compose.yml`, `.env.example` | Verify documented commands/configuration and build artifacts against current Gradle/Compose behavior |

## Step-by-Step Delivery Plan

### Phase 0 - Readiness and Baseline (must finish before feature tasks)

1. Trace each FR to the current Control UI/ViewModel, HTTP or gRPC client,
   backend route/service, and available test. Record implemented, partial,
   missing, and unverified behavior; avoid rewriting flows that already satisfy
   acceptance.
2. Confirm the pilot identity provider, device authorization flow, client
   configuration, token audience, and role/capability mapping. The README
   describes OIDC device authorization, while the core also exposes a separate
   username/password API; confirm which is the supported Control sign-in path.
3. Select and make available one representative supported physical device and
   gateway. Record model, firmware/software version, device tier, connection
   security, required permissions, and the person/environment responsible for
   the pilot test.
4. Trace a real run from the Control start action through backend run creation,
   gateway/device commands, device telemetry, run-state updates, and the live
   UI. Verify run ID/job ID mapping and source of truth. The documented REST
   start route uses an in-memory RegenOps store; do not assume it controls the
   firmware gRPC stream without evidence.
5. Reconcile deployment and developer instructions against actual Gradle
   project paths, Compose env-file resolution, and connection configuration.
   Current Android metadata says `0.1.0`; root and desktop say `1.0.0`; release
   docs reference legacy module selectors. `.env.example` sets `GRPC_PORT=9090`
   while Android's default gRPC port is `50051`. Define a consistent endpoint,
   intended v1.0.0 version values, and runnable commands before release work.
6. Establish the selected device's nominal telemetry cadence and a measurable
   stale-data/freshness threshold with the pilot owner. No numeric service
   latency or telemetry threshold exists in the reviewed docs.

**Exit gate**: The identity flow, named hardware target, end-to-end run/data
path, usable local run commands, canonical Compose environment-file path,
consistent HTTP/gRPC endpoint values, release version matrix, and telemetry
freshness threshold are written into the implementation task inputs. If the
hardware or identity provider is unavailable, record a blocker; do not represent
simulation tests as satisfying connected-device acceptance.

### Phase 1 - Entry, Authorization, and Protocol Selection (FR-001 to FR-004,
FR-012 to FR-015)

1. Establish the app entry state and distinguish unauthenticated,
authenticated, and demo sessions.
2. Complete or repair sign-in, cancellation, retry, logout/expired-session, and
   authorization error feedback using the Phase 0-selected identity flow.
3. Make demo mode explicit in the UI and keep demo provenance visible.
4. Verify protocol browse, search, refresh, selection, and version selection on
   Android and desktop, including loading, empty, and service-error states.
5. Test that permissions determine available actions without presenting a
   denied action as successful.

**Exit gate**: Story 1 and protocol-selection acceptance scenarios pass on both
app targets with no secret or sensitive real data in demo fixtures.

### Phase 2 - Simulated and Connected Run Lifecycle (FR-004 to FR-009,
FR-012 to FR-015, FR-017)

1. Complete the explicit pre-start review of protocol, version, run mode, and
   (for connected runs) selected device.
2. Verify simulated run creation and simulator telemetry without labeling the
   run as physical-device output.
3. Using the Phase 0 hardware configuration, complete the physical-device
   start and gateway connection path; preserve authorization, capability, and
   transport-security rules.
4. Display confirmed run identity/status and incoming observations; show
   connection/data freshness against the agreed threshold.
5. Verify pause and stop actions reflect server/device confirmation, including
   denied, rejected, timeout, reconnect, and unknown-state outcomes.
6. Exercise app background/restore and stream interruption/recovery without
   relabeling stale telemetry as current.

**Exit gate**: Stories 2 and 3 pass on Android and desktop in simulation. The
same representative physical device/gateway configuration passes a documented
end-to-end run through both the Android and desktop apps, including telemetry
reaching each app. No run is reported started or stopped without confirmation.

### Phase 3 - Run Review and Evidence Exports (FR-010 to FR-014)

1. Verify available run history is associated with the correct operator/session
   and run mode.
2. Verify report export for an authorized role and evidence-package export for
   the role permitted by the existing service policy.
3. Check filenames, run association, content type, integrity metadata, empty
   export, permission denial, and service-failure outcomes.
4. Confirm exported records do not expose secrets or unapproved sensitive data.

**Exit gate**: Story 4 passes for available records; denied, empty, and failing
exports have explicit failure outcomes and do not appear successful.

### Phase 4 - Cross-Target, Pilot, and Release Readiness (FR-015 to FR-017,
SC-001 to SC-007)

1. Run shared, backend, Android, and desktop tests using current Gradle project
   paths; run the root CI command.
2. Run Android UI tests and desktop manual/automated journey checks appropriate
   to the supported environments. Record any test requiring a physical device
   separately from emulator coverage.
3. Complete the 5-person pilot usability check: at least four operators finish
   the primary sign-in-or-demo, run, monitor, and stop journey without help,
   and at least four rate the run mode, current status, and telemetry connection
   state as understandable. Record outcomes without sensitive personal or
   biometric data.
4. Update the product README, release notes, pilot setup/checklist, and version
   metadata so Android, desktop, root, and release artifacts consistently
   identify v1.0.0. Verify release/build commands against actual Gradle paths.
5. Verify the app's release backup policy, TLS configuration, secret handling,
   demo-data provenance, evidence integrity, and release artifacts.
6. Produce a pilot acceptance record listing tested app targets, physical
   device/gateway configuration, test results, known limitations, and explicit
   non-clinical/non-production status.

**Release gate**: Do not label or distribute the controlled pilot as the
specified v1.0.0 MVP unless all P1 scenarios pass on Android and desktop, both
simulated and physical-device journeys pass, usability targets are met, exports
and security checks pass, and setup/limitations are documented.

## Design Decisions and Deferred Questions

- Reuse existing HTTP and protobuf interfaces; contract notes identify current
  paths but do not authorize incompatible API changes.
- Keep the pilot limited to one documented device/gateway configuration; do not
  imply fleet-wide support.
- Resolve an exact telemetry freshness target in Phase 0 from the selected
  device's cadence and pilot-owner expectations.
- Review accessibility and localization expectations during Phase 0; the
  current feature specification does not define supported locales or a formal
  accessibility conformance target.
- No database schema migration, new backend service, or dependency is planned.
  If Phase 0 demonstrates one is essential, create an explicit design decision,
  compatibility assessment, and rollback/migration plan before implementation.

## Complexity Tracking

No constitution violations or additional architectural complexity are proposed.
