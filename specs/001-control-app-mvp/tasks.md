# Tasks: RegenOps Control App MVP

**Input**: Design documents from `specs/001-control-app-mvp/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`contracts/`, `quickstart.md`, `readiness-matrix.md`

**Tests**: Test tasks are included because the Constitution and MVP success
criteria require automated behavior verification plus Android, desktop, and
physical-device acceptance evidence.

**Organization**: Tasks are grouped by prerequisite phase and user story.
Readiness gates are deliberately ahead of implementation; a hardware or
identity-provider blocker must be recorded rather than bypassed with simulation.

## Format

Every task uses `- [ ] T### [P?] [Story?] Description with file path`.
`[P]` means the task can run in parallel with other tasks in its phase, and
`[US#]` identifies the user story whose acceptance it supports.

## Phase 1: Setup and Baseline

**Purpose**: Establish repository evidence and runnable commands before changing
product behavior.

**Traceability**: Baseline coverage for FR-001 through FR-017 and SC-001 through
SC-007.

- [x] T001 Complete the implementation/evidence/status columns for FR-001 through FR-017 and SC-001 through SC-007 in `specs/001-control-app-mvp/readiness-matrix.md`; link each status to a source path and reproducible test or pilot evidence.
- [x] T002 Verify exact current Gradle test, Android variant, desktop run/package, and release task names from repository root; record executable commands and environment prerequisites in `specs/001-control-app-mvp/readiness-matrix.md`.
- [ ] T003 Verify Compose interpolation and service `env_file` resolution using `docker compose --env-file .env -f ops/docker-compose.yml config`; record the canonical root/ops environment-file path and required safe setup in `specs/001-control-app-mvp/readiness-matrix.md`.
- [x] T004 Reconcile the documented Control HTTP/gRPC host and port values across `.env.example`, `apps/control-kmp/androidApp/build.gradle.kts`, `ops/docker-compose.yml`, and `agents/device-gateway/src/main/kotlin/com/neogenesis/platform/gateway/DeviceGateway.kt`; record the selected values and each verified consumer in `specs/001-control-app-mvp/readiness-matrix.md`.

## Phase 2: Foundational Readiness Gates

**Purpose**: Resolve identity, physical-device, stream, and telemetry dependencies
that block all user-story implementation and valid connected acceptance.

**Traceability**: FR-001, FR-006, FR-008, FR-013, FR-014, FR-016, FR-017,
SC-004, SC-006, and SC-007.

- [ ] T005 Record the selected OIDC issuer, client, audience, token lifecycle, test roles, and mapping to backend roles/capabilities; update `specs/001-control-app-mvp/contracts/control-http.md` and `specs/001-control-app-mvp/readiness-matrix.md`. Block dependent auth work if a pilot identity provider/test account is unavailable.
- [ ] T006 Select and provision one representative supported physical device and gateway; record model, firmware, gateway version, device tier, TLS/mTLS mode, environment, and test owner in `docs/PILOT_INSTALL_CHECKLIST.md` and `specs/001-control-app-mvp/readiness-matrix.md`. Do not mark FR-017 passed without the device.
- [ ] T007 Trace a connected run from app request through backend acceptance, gateway command, device response, telemetry, and UI; record run/job/device identity mapping, source of truth, and missing links in `specs/001-control-app-mvp/contracts/device-streams.md` and `specs/001-control-app-mvp/readiness-matrix.md`.
- [ ] T008 Where T007 identifies a missing connected-run handoff, implement the smallest compatible integration in `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/RegenOpsHttpModule.kt` and `agents/device-gateway/src/main/kotlin/com/neogenesis/platform/gateway/DeviceGateway.kt`; preserve `shared/proto/src/main/proto/regenops.proto` and `shared/proto/src/main/proto/firmware.proto` compatibility and document the chosen source of truth in `specs/001-control-app-mvp/contracts/device-streams.md`.
- [ ] T009 Add or update connected-run integration coverage for accepted start, rejected/unauthorized start, matching run/job identifiers, and telemetry delivery in `services/core-server/src/test/kotlin/com/neogenesis/platform/core/RunServiceTest.kt` and `agents/device-gateway/src/test/kotlin/com/neogenesis/platform/gateway/DeviceGatewayTest.kt`; run the exact tasks recorded in T002.
- [ ] T010 Agree the selected device's expected telemetry cadence and numeric stale-data threshold with the pilot owner; record the value, rationale, and measurement method in `specs/001-control-app-mvp/readiness-matrix.md` and `specs/001-control-app-mvp/quickstart.md`. Block final FR-008 acceptance until agreed.
- [ ] T011 Reconcile root, `VERSION`, Android, desktop, and changelog release values; record the required v1.0.0 values and exact artifact/task paths in `specs/001-control-app-mvp/readiness-matrix.md`. Do not bump version metadata until implementation and release gates pass.
- [x] T012 Run the current baseline `./gradlew test --no-daemon` and record the result and any pre-existing failures in `specs/001-control-app-mvp/readiness-matrix.md`.

**Checkpoint**: User-story implementation can begin after identity, endpoint/config,
and repository test commands are known. Connected-run implementation and
acceptance additionally require T006-T010 to be resolved; unresolved external
hardware/owner inputs are blockers, not passed tasks.

## Phase 3: User Story 1 - Access the Control App (Priority: P1)

**Goal**: An operator can sign in or enter a clearly identified demo session and
reach the starting view without mistaking a failed or unauthenticated state for
success.

**Independent Test**: On Android and desktop, verify successful OIDC access,
cancel/deny/expiry/retry outcomes, and separately labeled demo entry; verify no
protected view is shown as authenticated after failed access.

**Traceability**: FR-001, FR-002, FR-012, FR-013, FR-014, and SC-004/SC-006.

### Tests for User Story 1

- [ ] T013 [US1] Add common tests for OIDC authorization success, cancellation, expiry, retry, and token/session state in `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/data/oidc/OidcRepositoryTest.kt`.
- [ ] T014 [P] [US1] Add Android UI coverage for signed-out, successful authorization, cancellation/error, and demo-session labeling in `apps/control-kmp/androidApp/src/androidTest/java/com/neogenesis/platform/control/AuthFlowUiTest.kt`.
- [ ] T015 [P] [US1] Add desktop UI/integration coverage for signed-out, authorization outcome, and demo-session labeling in `apps/control-kmp/desktopApp/src/test/kotlin/com/neogenesis/platform/control/desktop/AuthFlowTest.kt`.

### Implementation for User Story 1

- [ ] T016 [US1] Complete the OIDC session lifecycle and map identity-provider responses to the verified session state in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/oidc/OidcRepository.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/oidc/OidcDeviceAuthService.kt`.
- [ ] T017 [US1] Implement explicit signed-out, authenticated, demo, cancellation, retry, and error state transitions in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt`.
- [x] T018 [US1] Render the entry/authentication/demo states and prevent demo identity from appearing as authenticated in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`.
- [ ] T019 [US1] Wire the verified authorization URL launch and callback/result handling to Android in `apps/control-kmp/androidApp/src/main/java/com/neogenesis/platform/control/android/MainActivity.kt`.
- [ ] T020 [US1] Wire desktop authorization launch/result behavior using the existing platform boundary in `apps/control-kmp/shared/src/jvmMain/kotlin/com/neogenesis/platform/control/platform/DesktopPlatformModule.kt` and `apps/control-kmp/desktopApp/src/jvmMain/kotlin/com/neogenesis/platform/control/desktop/Main.kt`.
- [ ] T021 [US1] Run T013-T015 and verify Story 1 independently on Android and desktop; record command/device outcomes in `specs/001-control-app-mvp/readiness-matrix.md`.

**Checkpoint**: Story 1 is accepted only when authenticated and demo sessions
remain distinguishable on both app targets and all failed authorization paths
remain visibly unauthenticated.

## Phase 4: User Story 2 - Select a Protocol and Start a Run (Priority: P1)

**Goal**: An operator can select an available protocol/version and start both a
simulated run and an authorized connected-device run with the correct provenance.

**Independent Test**: On Android and desktop, select a protocol/version and
successfully start a simulation. With T006's device/gateway, start a physical
run and confirm service acceptance plus the same run identity through the
connected path. Verify unavailable/unauthorized starts are not shown as active.

**Traceability**: FR-003 through FR-006, FR-012, FR-013, FR-015, FR-017, and
SC-001/SC-004.

### Tests for User Story 2

- [ ] T022 [US2] Add common API/ViewModel tests for protocol search, refresh, version selection, empty/error responses, and pre-start mode/configuration in `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModelTest.kt`.
- [ ] T023 [P] [US2] Add Android UI coverage for protocol search/version selection and both simulation/connected pre-start choices in `apps/control-kmp/androidApp/src/androidTest/java/com/neogenesis/platform/control/RunControlUiTest.kt`.
- [ ] T024 [P] [US2] Add desktop integration coverage for protocol/version selection and both run-mode choices in `apps/control-kmp/desktopApp/src/test/kotlin/com/neogenesis/platform/control/desktop/RunControlTest.kt`.
- [ ] T025 [P] [US2] Add backend contract tests for protocol availability and accepted, rejected, and unauthorized run-start behavior in `services/core-server/src/test/kotlin/com/neogenesis/platform/core/RegenOpsHttpModuleTest.kt`.

### Implementation for User Story 2

- [ ] T026 [US2] Implement protocol loading, search, refresh, selection, version selection, and distinct loading/empty/error state in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt`.
- [ ] T027 [US2] Render protocol search/list/detail/version selection and explicit pre-start protocol/version/mode/device summary in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt`.
- [ ] T028 [US2] Implement the pre-start checks and start-request result handling for simulation and connected modes in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt`.
- [x] T029 [US2] Ensure simulation requests and resulting runs retain explicit simulated provenance in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/SimulatorApi.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt`.
- [ ] T030 [US2] Connect accepted physical-run requests to the verified service/gateway handoff from T007-T009 without treating an in-memory or HTTP-only acknowledgement as physical execution in `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/RegenOpsHttpModule.kt` and `agents/device-gateway/src/main/kotlin/com/neogenesis/platform/gateway/DeviceGateway.kt`.
- [ ] T031 [US2] Run T022-T025 and validate simulation on Android and desktop plus the connected-device start on each target; record the selected hardware and observed run IDs in `specs/001-control-app-mvp/readiness-matrix.md`.

**Checkpoint**: Both run modes must work and remain distinguishable. Connected
acceptance requires physical-device telemetry evidence on Android and desktop.

## Phase 5: User Story 3 - Monitor and Control an Active Run (Priority: P1)

**Goal**: An operator sees confirmed run state and appropriately fresh telemetry,
can pause/stop when permitted, and sees interruption or failure rather than
stale data or a false success.

**Independent Test**: On each app target, monitor one simulation and one connected
run, verify fresh/delayed/disconnected states, pause and stop, and compare the
displayed state with service/device confirmation.

**Traceability**: FR-007 through FR-009, FR-012 through FR-015, and
SC-001/SC-003/SC-004.

### Tests for User Story 3

- [ ] T032 [US3] Add common tests for telemetry freshness threshold, delayed frames, disconnect/reconnect, stale observations, and run-state updates in `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModelTest.kt`.
- [ ] T033 [P] [US3] Add Android UI tests for active run identity/status, telemetry freshness, pause/stop confirmation, and failure behavior in `apps/control-kmp/androidApp/src/androidTest/java/com/neogenesis/platform/control/RunControlUiTest.kt`.
- [ ] T034 [P] [US3] Add desktop tests for active run status, telemetry interruption/recovery, and confirmed pause/stop in `apps/control-kmp/desktopApp/src/test/kotlin/com/neogenesis/platform/control/desktop/LiveRunTest.kt`.
- [ ] T035 [P] [US3] Add service/gateway integration tests for confirmed run transitions and telemetry association with the selected run/job identifier in `services/core-server/src/test/kotlin/com/neogenesis/platform/core/GrpcTelemetryStreamTest.kt` and `services/core-server/src/test/kotlin/com/neogenesis/platform/core/RunServiceTest.kt`.

### Implementation for User Story 3

- [ ] T036 [US3] Map source timestamps, receipt time, and stream connection state into telemetry UI state using the agreed threshold in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/stream/RegenOpsStreamClient.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt`.
- [ ] T037 [US3] Display run identity/status, telemetry freshness, and connection state without presenting stale samples as current in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/TelemetryChart.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`.
- [ ] T038 [US3] Gate pause and stop actions by the verified capabilities and current run state, updating the view only after confirmed service/device response in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt`.
- [ ] T039 [US3] Implement observable loading, timeout, rejection, reconnect, and unknown-state outcomes for pause/stop in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`.
- [ ] T040 [US3] Run T032-T035 and verify simulation and the T006 physical-device run on Android and desktop; record freshness measurements and confirmed terminal states in `specs/001-control-app-mvp/readiness-matrix.md`.

**Checkpoint**: Both app targets display confirmed run/device state. No action or
telemetry is represented as successful/current when it is denied, unconfirmed,
or stale.

## Phase 6: User Story 4 - Review Runs and Export Their Records (Priority: P2)

**Goal**: An operator selects the intended historical run and obtains its
authorized report/evidence package, or sees an explicit empty, denied, or error
outcome.

**Independent Test**: On Android and desktop, select a completed run, request CSV
and evidence ZIP, verify run association and integrity metadata, then repeat with
empty data, insufficient permissions, and a failed export.

**Traceability**: FR-010 through FR-014, FR-015, and SC-005/SC-006.

### Tests for User Story 4

- [ ] T041 [US4] Add common tests for history loading/selection and CSV/evidence export success, empty, denied, and failure states in `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModelTest.kt`.
- [ ] T042 [P] [US4] Add Android UI coverage for selecting a prior run and displaying report/evidence export outcomes in `apps/control-kmp/androidApp/src/androidTest/java/com/neogenesis/platform/control/ExportsUiTest.kt`.
- [ ] T043 [P] [US4] Add desktop coverage for history selection and report/evidence export outcomes in `apps/control-kmp/desktopApp/src/test/kotlin/com/neogenesis/platform/control/desktop/ExportsTest.kt`.
- [ ] T044 [P] [US4] Verify server export authorization, content type, run association, and integrity behavior in `services/core-server/src/test/kotlin/com/neogenesis/platform/core/EvidenceExportTest.kt`.

### Implementation for User Story 4

- [ ] T045 [US4] Implement run-history loading, selection, refresh, and empty/error states in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt` and `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsUiState.kt`.
- [ ] T046 [US4] Render run identity, mode, status, and report/evidence actions in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`.
- [ ] T047 [US4] Map report and evidence responses, filenames, content types, and failures to the selected run in `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/ExportsApi.kt`.
- [ ] T048 [US4] Run T041-T044 and validate exports for an authorized operator and the service-permitted evidence role on Android and desktop; record run IDs and outcomes in `specs/001-control-app-mvp/readiness-matrix.md`.

**Checkpoint**: Report and evidence exports match the selected run and the
existing authorization rules; no failed or empty export is reported as success.

## Phase 7: Polish, Pilot Acceptance, and Release

**Purpose**: Complete shared acceptance criteria, documentation, security checks,
and controlled-pilot release evidence.

**Traceability**: FR-014 through FR-017 and SC-001 through SC-007.

- [ ] T049 Review the Android, desktop, and shared app paths for secret or sensitive biometric/patient-data exposure; record findings and remediation in `specs/001-control-app-mvp/readiness-matrix.md` and `SECURITY.md` only if policy documentation needs correction.
- [x] T050 Run `./gradlew test --no-daemon` and the verified focused Android, desktop, and core-server test/build tasks; record exact commands and results in `specs/001-control-app-mvp/readiness-matrix.md`.
- [ ] T051 Run a five-operator usability session using Android and desktop builds; record target and outcomes, verifying at least four operators complete the primary flow unaided and at least four understand run mode, status, and connection state in `specs/001-control-app-mvp/readiness-matrix.md`.
- [ ] T052 Reconcile v1.0.0 root, `VERSION`, Android, desktop, changelog, and release artifact metadata only after all MVP gates pass in `build.gradle.kts`, `VERSION`, `apps/control-kmp/androidApp/build.gradle.kts`, `apps/control-kmp/desktopApp/build.gradle.kts`, and `CHANGELOG.md`.
- [ ] T053 Update the root operator quickstart for the validated MVP configuration, endpoints, exact commands, supported user journeys, demo/connected distinctions, and limitations in `README.md`.
- [ ] T054 Update the controlled-pilot deployment, security, release, and device setup instructions to match verified Compose/env-file behavior and the validated device/gateway in `docs/PILOT_INSTALL_CHECKLIST.md`, `docs/RELEASING.md`, and `ops/docker-compose.yml`; change Compose configuration only if T003 confirms a defect.
- [ ] T055 Produce the controlled-pilot acceptance record, including both app targets, simulator and physical-device evidence, named device/gateway configuration, usability results, known limitations, and explicit non-clinical/non-production statement in `specs/001-control-app-mvp/readiness-matrix.md`.
- [ ] T056 Verify the Android release backup policy and release artifact/signing checks against `apps/control-kmp/androidApp/src/main/AndroidManifest.xml`, `apps/control-kmp/androidApp/src/main/res/xml/backup_rules.xml`, `apps/control-kmp/androidApp/src/main/res/xml/data_extraction_rules.xml`, and the verified Gradle release tasks; attach results to `specs/001-control-app-mvp/readiness-matrix.md`.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No application changes; establish traceability and verified local commands.
- **Foundational (Phase 2)**: Depends on setup; blocks valid user-story implementation where identity, endpoints, or contract behavior is unresolved. Physical-device acceptance is blocked until the pilot owner provides hardware and configuration.
- **US1 (Phase 3)**: Depends on T005 identity-provider mapping and Phase 1 baseline.
- **US2 (Phase 4)**: Depends on US1 session state and T005. Simulated flow can be implemented independently once the baseline is ready; connected flow and its acceptance depend on T006-T010.
- **US3 (Phase 5)**: Depends on US2 run creation. Physical telemetry acceptance depends on T006-T010 and connected-run completion.
- **US4 (Phase 6)**: Depends on run identity/history from US2 and completed run records from US3; can use existing seeded/demo records for early UI work.
- **Polish (Phase 7)**: Depends on all four stories and all physical-device gates.

### User Story Dependencies

- **US1 (P1)**: Independent entry/auth journey; blocks authenticated protocol and run interactions.
- **US2 (P1)**: Depends on US1 for authorization; delivers protocol selection and both run-start modes.
- **US3 (P1)**: Depends on US2 run start; delivers monitoring and controls.
- **US4 (P2)**: Depends on run/history/export data from US2/US3; remains independently testable using seeded completed runs for UI and contract tests.

### Parallel Opportunities

- T001-T004 should be executed serially because they update the shared readiness matrix and some configuration surfaces overlap.
- T005, T006, T010, and T011 are independent external/readiness decisions, but T007-T009 depend on available integration evidence and hardware details.
- Within each story, platform-specific tests (Android vs desktop) can be developed in parallel when they do not edit the same files; shared ViewModel tests are serialized across stories because they use the same test file.
- Backend/gateway contract verification can run alongside client UI work after Phase 2 has established the correct boundary and identifiers.
- Avoid parallel edits to shared `RegenOpsViewModel.kt`, `RegenOpsUiState.kt`, and `Screens.kt`; sequence those shared-state tasks or assign one integrator.

## Parallel Example: User Story 1

```text
T013 OIDC shared tests in apps/control-kmp/shared/src/commonTest/.../OidcRepositoryTest.kt
T014 Android auth UI tests in apps/control-kmp/androidApp/src/androidTest/.../AuthFlowUiTest.kt
T015 Desktop auth tests in apps/control-kmp/desktopApp/src/test/.../AuthFlowTest.kt
```

## Implementation Strategy

1. Finish the repository baseline and Phase 2 readiness gates; record external blockers explicitly.
2. Implement US1 and validate entry/auth/demo on both targets.
3. Implement US2's simulation path and then connected-device path; never count the simulation as hardware acceptance.
4. Implement US3 monitoring and safe controls; confirm both source freshness and run-state confirmation.
5. Implement US4 history and exports.
6. Complete cross-target tests, the five-operator usability check, security/release checks, documentation, and version alignment.
7. Declare v1.0.0 pilot acceptance only when every release gate in `specs/001-control-app-mvp/plan.md` and every criterion in `specs/001-control-app-mvp/spec.md` is evidenced.

## Notes

- All tasks include a task ID, checkbox, exact existing or planned file path, and story label where applicable.
- `[P]` is used only for independent test/evidence tasks or separate baseline surfaces.
- Test tasks are required by the specification's measurable acceptance criteria and project Constitution.
- Tasks T006, T010, T051, and the connected portions of T008-T009/T031/T040 cannot be marked complete without the named owner/device/measurement evidence.
- Do not create new modules, replace APIs, change protobuf compatibility, or bump versions unless a readiness gap requires it and the corresponding task is explicitly reviewed.
