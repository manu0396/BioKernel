# Feature Specification: RegenOps Control App MVP

**Feature Branch**: `001-control-app-mvp`

**Created**: 2026-10-16

**Status**: Draft

**Input**: User description: "I would like to have a version 1.0.0 of th app to see the basic MVP of the maain app. Strcuture the work documentating everything firts and setting all the steps to have a basic MVP for the main goal of this app. You can find documentation in current README.md file and other md files added in the projects. GO step by step"

## Clarifications

### Session 2026-10-16

- Q: Must the v1.0.0 MVP run protocols on a real connected device, or is a simulator-based demonstration sufficient? → A: Both simulated runs and real connected-device runs are required for v1.0.0.
- Q: Should the app’s v1.0.0 MVP be released for a controlled pilot/demo, or must it be ready for general production use? → A: Controlled pilot/demo release; it is not a claim of clinical or unrestricted production readiness.
- Q: Which application workflow should this MVP cover: the RegenOps Control app described in the README, or a BioKernel retinal-analysis workflow? → A: RegenOps Control (operator sign-in, protocols, connected/simulated runs, telemetry, and evidence export).
- Q: What is the minimum real-device coverage required to accept the connected-run part of v1.0.0? → A: At least one representative supported physical device and gateway must pass an end-to-end run, with the validated configuration documented.
- Q: Should v1.0.0 require a five-operator usability check, with at least four completing the main run journey unaided? → A: Yes; at least 4 of 5 representative operators must complete the primary journey without help.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Access the Control App (Priority: P1)

An operator opens the main Control app, signs in through the supported authorization flow, and reaches an understandable starting point for managing a run. A demo path lets an operator evaluate the experience without connecting a physical device.

**Why this priority**: Access and a usable first screen are prerequisites for every MVP journey. A demo path makes the basic app demonstrable without requiring a pilot installation.

**Independent Test**: Starting from a fresh app session, verify that an operator can complete authorization or enter the explicitly identified demo mode, see the available starting actions, and receive a clear outcome if access cannot be completed.

**Acceptance Scenarios**:

1. **Given** an unauthenticated operator and a reachable authorization service, **When** the operator completes the supported sign-in flow, **Then** the app shows the authenticated starting view.
2. **Given** an operator who declines, cancels, or cannot complete authorization, **When** the flow ends, **Then** the app explains the state and offers a safe retry without showing an authenticated view.
3. **Given** demo mode is available, **When** an operator chooses it, **Then** the app identifies the session as a demonstration and does not imply that a physical device is connected.

### User Story 2 - Select a Protocol and Start a Run (Priority: P1)

An authorized operator browses available protocols, selects a protocol and version, and starts either a simulated run or a permitted connected run. Before starting, the app makes the selected protocol, version, and run mode clear.

**Why this priority**: Selecting and starting a run is the primary Control app task and establishes the MVP's core user value.

**Independent Test**: Verify both run modes: with demo services, select a protocol version and start a simulated run; with an authorized connected device, select a protocol version and start a real run. Confirm the selected configuration and resulting run for each mode, and verify that an unavailable or unauthorized connected run is not reported as started.

**Acceptance Scenarios**:

1. **Given** available protocols, **When** the operator searches, refreshes, and selects one, **Then** the app shows its available versions and allows a version to be selected.
2. **Given** a selected protocol and version, **When** the operator starts a simulated run, **Then** the app identifies it as simulated and opens the active run view.
3. **Given** a selected protocol and version and an authorized connected device, **When** the operator starts a connected run, **Then** the app reports the returned run state and opens the active run view only after the run is accepted.
4. **Given** there are no protocols or the service is unavailable, **When** the operator opens or refreshes the list, **Then** the app distinguishes an empty result from a loading or service error and provides an appropriate recovery action.
5. **Given** one representative supported physical device is connected through its gateway, **When** an authorized operator starts and monitors a run, **Then** the connected-device journey completes end to end and the validated device configuration is recorded for the pilot.

### User Story 3 - Monitor and Control an Active Run (Priority: P1)

An operator views the active run's identity, status, and incoming telemetry, can pause or stop it when permitted, and can understand when data is delayed or the connection is interrupted.

**Why this priority**: Operators need useful feedback and safe controls while a run is active; a start-only demo would not demonstrate the main app's purpose.

**Independent Test**: Start a simulated run, verify that its status and incoming observations are visible, pause it, stop it, and verify the final state. Interrupt the data connection and verify the app identifies stale or interrupted data rather than presenting it as current.

**Acceptance Scenarios**:

1. **Given** an active run with incoming telemetry, **When** the operator views it, **Then** the app presents the run status and received observations with enough context to identify the run.
2. **Given** an active run that permits pausing, **When** the operator requests pause, **Then** the app shows the resulting state only after receiving confirmation and reports a failure if the request is rejected.
3. **Given** an active run, **When** the operator requests stop, **Then** the app clearly communicates the confirmed terminal state and prevents the stopped run from appearing active.
4. **Given** telemetry is delayed or its connection is interrupted, **When** new observations stop arriving, **Then** the app indicates the connection/data state and does not present old observations as newly received.

### User Story 4 - Review Runs and Export Their Records (Priority: P2)

An operator reviews available recent runs and exports a report or evidence package for a selected run, with a clear result when the export is unavailable or unsuccessful.

**Why this priority**: Run review and export support pilot follow-up and evidence handling, after the primary start-and-monitor journey works.

**Independent Test**: Select a completed run with available records, export its report and evidence package, and verify that each download is associated with the selected run and has an explicit success or failure outcome.

**Acceptance Scenarios**:

1. **Given** prior runs are available, **When** the operator opens or refreshes the run list, **Then** the app shows each run's identifying information and status.
2. **Given** a run with an available report, **When** the operator requests the report, **Then** the app provides the report or clearly explains why it could not be produced.
3. **Given** a run with an available evidence package, **When** the operator requests it, **Then** the app provides the package and identifies the run to which it belongs.
4. **Given** a run has no exportable records or the export service fails, **When** the operator requests an export, **Then** the app does not report a successful export and gives a recoverable, understandable outcome.
5. **Given** an MVP release candidate and its demonstration materials, **When** a reviewer checks them for exposed credentials or sensitive biometric, retinal, or patient data, **Then** none is present.
6. **Given** a new project contributor and the MVP release documentation, **When** the contributor follows its configuration, run, and verification instructions, **Then** each documented step can be completed without undocumented setup.

### Edge Cases

- Authorization is cancelled, expires, is denied, or the authorization service is unreachable.
- The protocol list is empty, partially loaded, or unavailable; a refresh may also fail.
- A selected protocol version becomes unavailable before a run is started.
- Starting, pausing, or stopping a run is rejected, times out, or returns an unknown state.
- The app is closed or backgrounded during a run, then reopened after the run state has changed.
- Telemetry is empty, delayed, interrupted, duplicated, or resumes after a connection loss.
- A selected run has incomplete records, an unavailable export, or an integrity-verification failure.
- The operator lacks permission for a device or action; the app must not imply that the action succeeded.
- Demo and connected-run data appear in the same session; the app must keep their mode and provenance distinguishable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST provide an identifiable sign-in flow and show whether the operator is authenticated.
- **FR-002**: The app MUST distinguish a demonstration session from a connected-device session throughout run selection, monitoring, and review.
- **FR-003**: The app MUST let an authorized operator browse, search, and refresh available protocols and select an available version.
- **FR-004**: The app MUST show the selected protocol, version, and run mode before a run is started.
- **FR-005**: The app MUST let an operator start a simulated run without representing it as a physical-device run.
- **FR-006**: The v1.0.0 MVP MUST support starting a real connected-device run, and MUST let an authorized operator do so only when the selected action is permitted and the service accepts the request. Simulation alone does not satisfy this requirement.
- **FR-007**: The app MUST identify the active run and present its current confirmed status and received telemetry.
- **FR-008**: The app MUST clearly identify delayed or interrupted telemetry and MUST NOT imply that stale observations are current.
- **FR-009**: The app MUST expose pause and stop actions only when applicable and communicate their confirmed outcomes or failures.
- **FR-010**: The app MUST provide a view of available prior runs with enough identifying information to select the intended run.
- **FR-011**: The app MUST provide available run reports and evidence packages for the selected run, with an explicit success or failure outcome.
- **FR-012**: The app MUST provide distinct loading, empty, success, and error feedback for the primary protocol, run, telemetry, and export journeys.
- **FR-013**: The app MUST preserve authorization and permission boundaries and MUST NOT report a protected action as successful when it is denied or unconfirmed.
- **FR-014**: The app and its demonstration materials MUST NOT expose credentials or sensitive biometric, retinal, or patient data.
- **FR-015**: The MVP MUST support the documented Android and desktop app experiences for the primary user journeys.
- **FR-016**: The release documentation MUST identify the app as a controlled pilot/demo, explicitly state that it is not a claim of clinical or unrestricted production readiness, and document MVP scope, supported user journeys, known limitations, required configuration, and steps to build, run, and verify the app.
- **FR-017**: The v1.0.0 pilot MUST validate an end-to-end run using at least one representative supported physical device and gateway, and MUST document the validated device configuration.

### Key Entities *(include if feature involves data)*

- **Operator Session**: The current operator's authenticated or demonstration state and the permissions relevant to available actions.
- **Protocol and Version**: A selectable definition for a run, including its identity and version.
- **Run**: An identified simulated or connected execution with a selected protocol, status, timestamps, and available records.
- **Telemetry Observation**: A time-associated observation belonging to a run, with enough receipt context to distinguish current data from delayed or interrupted data.
- **Evidence Package**: An export associated with a selected run that contains its evidence records and integrity information when available.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All P1 acceptance scenarios pass on both documented app targets before the MVP is declared demo-ready.
- **SC-002**: At least 4 of 5 representative pilot operators complete the sign-in-or-demo, simulated-run, live-monitoring, and stop journey without facilitator intervention in a usability session.
- **SC-003**: At least 4 of 5 representative pilot operators rate the run mode, current run status, and telemetry connection state as understandable after completing the primary journey.
- **SC-004**: Every tested denied, failed, empty, or interrupted primary-flow scenario produces an explicit non-success outcome; none is displayed as a successful action.
- **SC-005**: A reviewer can export a report and evidence package for a run with available records and verify that both correspond to the selected run.
- **SC-006**: Release review finds no committed secrets or sensitive biometric, retinal, or patient data in the MVP source, fixtures, logs, or demonstration artifacts.
- **SC-007**: A new project contributor can follow the release documentation to configure, run, and execute the documented verification steps without relying on undocumented setup.

## Assumptions

- This specification covers the RegenOps Control app described in the root README, rather than the separate retinal-analysis domain models present in the repository.
- Version 1.0.0 in this request means the desired MVP scope. The repository already contains `1.0.0` version and changelog entries; this specification does not assert that those entries prove MVP acceptance or change release metadata.
- The v1.0.0 MVP is a controlled pilot/demo product, not a claim of clinical diagnosis, treatment guidance, regulatory certification, or unrestricted production readiness.
- The existing authorization flow, protocol/run services, telemetry stream, simulator, connected-device integration, and export capabilities are dependencies to assess during planning; this specification does not require a rewrite or new service.
- One representative supported physical device and gateway are the minimum connected-device validation scope; support for every device model or tier is not implied.
- Android and desktop are in scope because both are documented app targets. Other platforms are outside this MVP.
- At least 4 of 5 representative pilot operators completing the primary journey unaided is a v1.0.0 acceptance target.
- Existing documentation is the source for intended behavior. In particular, this scope reflects `README.md`, `docs/ARCHITECTURE.md`, `docs/ADR/0001-kmp-ktor-grpc.md`, `docs/qa/UI_ACTIONS_IMPLEMENTED.md`, `docs/PILOT_INSTALL_CHECKLIST.md`, `docs/REGULATORY_TRACEABILITY.md`, `docs/ANDROID_BACKUP_POLICY.md`, `docs/RELEASING.md`, `CONTRIBUTING.md`, and `SECURITY.md`.
