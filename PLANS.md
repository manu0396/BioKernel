# ExecPlan: Implement RegenOps Control App MVP (2026-10-16)

## Follow-up: Resolve Android OIDC Build Configuration

### Milestones
1) Trace Android OIDC values from Gradle configuration to `AppConfig` and identify missing local configuration sources.
2) Load non-committed root `.env` values for Android builds, preserve explicit Gradle/local/environment overrides, and safely encode configured strings in `BuildConfig`.
3) Document how to configure and rebuild/sync the Android app; validate generated BuildConfig and the affected demo build.

### File-by-File Change List
- `apps/control-kmp/androidApp/build.gradle.kts`: Load `.env` values as a fallback and escape BuildConfig string literals.
- `.env.example`, `README.md`: Document Android OIDC configuration sources and how to regenerate the app configuration.
- `PLANS.md`: Record diagnosis, verification, and rollback.

### Verification Commands
- `./gradlew :apps:control-kmp:androidApp:generateDemoGoogleDebugBuildConfig -POIDC_ISSUER=https://issuer.example -POIDC_CLIENT_ID=example-client --rerun-tasks`
- Inspect generated BuildConfig using only synthetic test values.
- `./gradlew :apps:control-kmp:androidApp:assembleDemoGoogleDebug -PDEMO_MODE=true --rerun-tasks --no-daemon --console=plain`
- `git diff --check`

### Rollback Plan
- Revert only the `.env` loading/BuildConfig encoding changes and associated README/plan text; preserve existing uncommitted work and local configuration files.

### Progress
- [x] Trace confirmed Android reads `local.properties`, Gradle properties, and process environment, but not the root `.env` copied as instructed in the README; this checkout has no `.env` and no local OIDC client ID key.
- [x] Add root `.env` fallback after existing local properties, Gradle properties, and process environment; safely encode configured strings and document Android rebuild/sync steps.
- [x] Verify generated BuildConfig using both synthetic Gradle properties and a temporary synthetic root `.env`; the demo variant assembles successfully. Removed the temporary `.env` after verification.

## Milestones
1) Establish repository, runtime, device/emulator, and Gradle baselines; populate the readiness matrix with observed facts.
2) Resolve available identity, device/gateway, run handoff, and telemetry prerequisites; record externally blocked gates without fabricating acceptance evidence.
3) Implement and test the stories in order: access, protocol/run start, monitoring/control, then history/export.
4) Complete security, build/release, documentation, and available virtual-device acceptance checks; reconcile task status and evidence.
5) Remove duplicate gateway/KMP source-root ownership reported by Android Studio, validate module boundaries, and smoke-test on the connected Moto G55.
6) Add deterministic OIDC and device/gateway telemetry test doubles; keep telemetry freshness acceptance blocked until a pilot owner agrees the cadence and threshold. Set Android app metadata to 1.0.0 while clearly retaining the controlled-pilot blockers.

## File-by-File Change List
- `PLANS.md`: Track implementation milestones, exact project/test commands, and rollback.
- `specs/001-control-app-mvp/tasks.md`: Mark only evidenced tasks complete and correct any discovered path or dependency errors.
- `specs/001-control-app-mvp/readiness-matrix.md`: Record baseline, task evidence, virtual-device results, and unresolved owner/hardware blockers.
- `specs/001-control-app-mvp/contracts/`, `specs/001-control-app-mvp/quickstart.md`, and `docs/PILOT_INSTALL_CHECKLIST.md`: Record verified integration/configuration and pilot setup decisions.
- Existing Android, desktop, shared KMP, core-server, gateway, and test files named by the implementation tasks: Make only changes supported by the readiness findings.
- `README.md`, `CHANGELOG.md`, version metadata, and `ops/docker-compose.yml`: Update only when the corresponding release/configuration gates pass.
- `services/core-server/build.gradle.kts`: Stop importing the separately included device-gateway module's main/test directories as core-server source roots.
- `apps/control-kmp/shared/build.gradle.kts`: Replace manually aggregated `shared/{domain,data,network}` source directories with project dependencies, leaving source ownership with each module.
- `apps/control-kmp/androidApp/build.gradle.kts`, `apps/control-kmp/desktopApp/build.gradle.kts`, `build.gradle.kts`, `VERSION`, `CHANGELOG.md`: Align 1.0.0 artifact metadata and record a mock-backed demo/test build; do not tag or describe it as pilot acceptance.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/oidc/OidcRepository.kt`: Remove the production mock-auth bypass so OIDC success can only come from a configured endpoint.
- `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/data/oidc/OidcRepositoryTest.kt`, `agents/device-gateway/src/test/kotlin/com/neogenesis/platform/gateway/DeviceGatewayTest.kt`, `services/core-server/src/test/kotlin/com/neogenesis/platform/core/MockGatewayTelemetryFlowTest.kt`: Add deterministic fake identity, gateway, and telemetry behavior with synthetic-only data.
- `agents/device-gateway/src/main/kotlin/com/neogenesis/platform/gateway/DeviceGateway.kt`: Extract and reuse a deterministic tick payload builder for unit tests without changing its simulated operating mode.
- `README.md`, `CHANGELOG.md`, `docs/RELEASING.md`, `specs/001-control-app-mvp/quickstart.md`: Document mock-profile setup and reproducible verification, and distinguish mocked acceptance from the still-required real pilot checks.
- `specs/001-control-app-mvp/readiness-matrix.md`, `specs/001-control-app-mvp/quickstart.md`, `.env.example`, `README.md`: Document mock-profile setup and reproducible verification, and distinguish mocked acceptance from the still-required real pilot checks.

## Verification Commands
- `./gradlew test --no-daemon`
- `./gradlew :apps:control-kmp:androidApp:assembleDebug`
- `./gradlew :apps:control-kmp:shared:allTests`
- `./gradlew :apps:control-kmp:desktopApp:test`
- `./gradlew :services:core-server:test`
- `./gradlew :agents:device-gateway:test`
- `./gradlew :services:core-server:test :agents:device-gateway:test`
- `./gradlew :apps:control-kmp:shared:allTests :apps:control-kmp:androidApp:assembleDemoGoogleDebug :apps:control-kmp:desktopApp:compileKotlin`
- Install and launch the demo APK on the attached Moto G55 5G using ADB; verify process stays alive and the explicit demo entry is rendered.
- `./gradlew test --no-daemon`
- `./gradlew :apps:control-kmp:shared:allTests :apps:control-kmp:androidApp:connectedDemoGoogleDebugAndroidTest :apps:control-kmp:desktopApp:test :services:core-server:test :agents:device-gateway:test --no-daemon --console=plain`
- Verify mock profile reports its provenance and never claims authenticated OIDC, physical device execution, or pilot acceptance.
- `docker compose --env-file .env -f ops/docker-compose.yml config` (only when a safe local `.env` is available)
- Android virtual-device install/launch and user-flow checks, using an already available emulator/AVD; do not claim physical-device acceptance from emulator evidence.
- `git diff --check`

## Rollback Plan
- Revert only implementation and test changes introduced for this MVP; preserve pre-existing staged/unstaged work and the existing generated specification/Spec Kit files.
- Restore task checkboxes and readiness entries to their prior state if implementation is rolled back; retain observed baseline failures and external blockers as evidence.
- Do not remove, overwrite, or reset unrelated repository files; do not roll back version or deployment configuration without confirming it was changed for this plan.

## Progress
- [x] Establish readiness/baseline and confirm virtual-device availability; the post-change root test suite and focused shared JVM tests pass.
- [x] Record the external identity-provider, physical-device/gateway, run-handoff, and telemetry-threshold blockers.
- [x] Implement a clearly labeled unauthenticated demo session and isolated synthetic-run paths; add common, JVM, and Android coverage.
- [x] Run all 7 Android instrumentation tests successfully on `Medium_Phone` (API 36), forcing `DEMO_MODE=true` in generated BuildConfig.
- [x] Remove duplicate device-gateway/KMP source roots; the demo Android assembly, core-server tests, and root test suite pass. Android Studio Gradle sync is still needed to refresh the IDE model.
- [x] Install and launch the demo build on the attached Moto G55 5G (Android 16/API 36); all 7 instrumentation tests passed on both the phone and AVD. Record handset evidence without presenting it as lab-device/gateway acceptance.
- [x] Remove the production `http://mock-auth` token shortcut and add Ktor MockEngine authorization, pending, success, and denial tests; add deterministic gateway tick and run-filtered core telemetry tests.
- [x] Align Android metadata to `1.0.0`/versionCode 2 and document the mock/demo-only scope plus outstanding controlled-pilot gates.
- [x] Verify shared multiplatform/root tests, core-server/gateway tests, desktop compilation, and the Android demo instrumentation suite (7/7 on both the Moto G55 and Medium Phone AVD).
- [ ] Implement and verify US1 through US4 end-to-end in dependency order; connected, monitoring, and export acceptance remain incomplete.
- [ ] Complete applicable pilot/release gates and reconcile every task/evidence status.

# ExecPlan: Specify the RegenOps Control App MVP (2026-10-16)

## Milestones
1) Review project product, architecture, UX, security, and release documentation.
2) Write a bounded, testable v1.0.0 MVP specification for the main Control app.
3) Clarify the release posture, app workflow, hardware validation scope, and usability gate.
4) Create and review the Spec Kit requirements-quality checklist and persist feature location.
5) Validate scope, acceptance scenarios, measurable outcomes, and generated artifacts.

## File-by-File Change List
- `specs/001-control-app-mvp/spec.md`: Specify user-facing MVP scope, requirements, success criteria, assumptions, and boundaries.
- `specs/001-control-app-mvp/checklists/requirements.md`: Record specification-quality review results.
- `.specify/feature.json`: Persist the feature directory for downstream Spec Kit commands.
- `PLANS.md`: Track specification work and validation.
- `specs/001-control-app-mvp/plan.md`: Step-by-step technical delivery plan, design gates, and phases.
- `specs/001-control-app-mvp/research.md`: Repository-grounded decisions, constraints, and unresolved planning risks.
- `specs/001-control-app-mvp/data-model.md`: MVP-facing entities, relationships, and lifecycle/state expectations.
- `specs/001-control-app-mvp/contracts/`: Compatibility surfaces to validate against existing contracts and endpoints.
- `specs/001-control-app-mvp/quickstart.md`: End-to-end pilot validation sequence.
- `specs/001-control-app-mvp/readiness-matrix.md`: Requirement-to-code/evidence audit and pilot/release blockers.
- `specs/001-control-app-mvp/tasks.md`: Dependency-ordered implementation tasks grouped by readiness and user story.
- `PLANS.md`: Track planning artifact generation and validation.

## Verification Commands
- `.specify/scripts/bash/resolve-template.sh spec-template --json`
- `.specify/scripts/bash/resolve-template.sh checklist-template --json`
- `.specify/scripts/bash/setup-plan.sh --json`
- `git diff --check`
- Validate plan/design artifact consistency, no template placeholders, checklist findings, and feature metadata.

## Rollback Plan
- Remove only the plan/research/design/contract/quickstart artifacts created for `001-control-app-mvp` and revert this ExecPlan entry. Preserve the pre-existing specification, requirements checklist, `.specify/feature.json`, Spec Kit installation files, and all unrelated worktree changes.

## Progress
- [x] Reviewed README, architecture, ADR, UI action inventory, security, release, pilot, and regulatory documentation.
- [x] Create the v1.0.0 Control app MVP specification and requirements checklist.
- [x] Clarified that both simulated and real connected-device runs are required.
- [x] Clarified controlled pilot/demo release, RegenOps Control workflow, one representative physical device/gateway, and the 4-of-5 usability gate.
- [x] Generated repository-grounded implementation phases, research decisions, data model, HTTP/gRPC compatibility notes, and pilot quickstart.
- [x] Generate task list by readiness gate and user story, with exact file paths and independent acceptance checks.
- [x] Validate task IDs, labels, paths, dependencies, requirement coverage, and story-level testability.

# ExecPlan: Establish the BioKernel Project Constitution (2026-10-16)

## Milestones
1) Resolve the active Spec Kit constitution template and derive guardrails from existing project docs.
2) Replace the generated constitution scaffold with repo-grounded principles and governance.
3) Validate placeholders, dates, version report, and final diff.

## File-by-File Change List
- `.specify/memory/constitution.md`: Define project principles, security constraints, development workflow, and governance.
- `PLANS.md`: Track this constitution task and its validation.

## Verification Commands
- `.specify/scripts/bash/resolve-template.sh constitution-template --json`
- `git diff --check`
- Search `.specify/memory/constitution.md` for unresolved placeholders and validate its version/date metadata.

## Rollback Plan
- Restore only `.specify/memory/constitution.md` to the generated scaffold and remove this ExecPlan entry; preserve all unrelated worktree changes.

## Progress
- [x] Resolved the active template and reviewed README, contributing guidance, security policy, and CI conventions.
- [x] Replace constitution scaffold using established project practices.
- [x] Validate constitution structure and metadata; the original ratification date remains an explicit TODO.

# ExecPlan: Adopt GitHub Spec Kit in the Existing Repository (2026-10-16)

## Milestones
1) Confirm Spec Kit CLI availability and identify existing project-file conflicts.
2) Initialize Spec Kit in place for GitHub Copilot without changing application code.
3) Review generated files and validate the CLI/project setup.

## File-by-File Change List
- `PLANS.md`: Record this adoption plan and progress.
- `.specify/` and Copilot integration skill/command files: Add Spec Kit's shared project configuration, templates, scripts, and workflow skills.

## Verification Commands
- `specify --version`
- `specify check`
- `git status --short`
- `git diff --check`

## Rollback Plan
- Remove only the Spec Kit files introduced by this initialization, after verifying their paths against the resulting diff/status.
- Leave all pre-existing work untouched, including the staged deletion of `fix_docx_autonomo_preslu.py`.

## Progress
- [x] Confirmed the Spec Kit CLI is installed and no tracked `.specify` or Copilot Spec Kit integration files exist.
- [x] Initialize the existing repository for GitHub Copilot.
- [x] Review generated files and validate setup with `specify check`; no application source files were changed.

# ExecPlan: Resolve CI Build and Test Failures (2026-10-15)

## Root Causes
1. **Missing contracts dependency**: CI failed with `Could not find com.neogenesis:neogenesis-contracts:1.0.0` because `agents/device-gateway`, `services/core-server`, and `apps/control-kmp/shared` referenced an external `mavenLocal()` dependency for `com.neogenesis.grpc` protobuf/gRPC stubs. When `shared:proto` was restructured, `bioprint.proto` was missing from `shared/proto/src/main/proto/`, forcing consumers to search for the published artifact in the local maven cache.
2. **Linux filesystem timestamp coarseness in `GrpcMtlsTest`**: Fast sequential file modifications during unit tests resulted in identical `lastModified` millisecond timestamps, preventing `TlsFileWatcher` from detecting updated TLS certificates on GitHub Actions runners.
3. **Asynchronous race condition in `RegenOpsViewModelTest`**: In `startRunNavigatesToLive` and `startSimulatedRunNavigatesToLive`, the ViewModel launched an asynchronous coroutine on `Dispatchers.Default`. Because mocked API calls returned immediately, the coroutine could finish and set `isStartingRun = false` before the calling test thread evaluated `assertTrue(viewModel.state.value.isStartingRun)`.

## Milestones
1) Restore `bioprint.proto` into `shared/proto/src/main/proto/` to compile `com.neogenesis.grpc` directly in `:shared:proto`.
2) Remove unresolvable `libs.neogenesis.contracts` dependency from `agents/device-gateway/build.gradle.kts`, `services/core-server/build.gradle.kts`, `apps/control-kmp/shared/build.gradle.kts`, and `gradle/libs.versions.toml`.
3) Ensure Gradle wrapper executable permission (`chmod +x gradlew`).
4) Harden `TlsFileWatcher` to check both file size and `lastModified`, and ensure test files have distinct timestamps.
5) Harden `RegenOpsViewModelTest` with `CompletableDeferred` gating to deterministically synchronize `isStartingRun` and `LIVE_RUN` screen transitions without race conditions.
6) Verify build and full test suite with `./gradlew test --no-daemon` locally and in GitHub Actions CI.

## File-by-File Change List
- `shared/proto/src/main/proto/bioprint.proto`: Add gRPC/Protobuf definitions for `com.neogenesis.grpc` services and records.
- `agents/device-gateway/build.gradle.kts`: Remove `libs.neogenesis.contracts` dependency; use `:shared:proto`.
- `services/core-server/build.gradle.kts`: Remove `libs.neogenesis.contracts` dependency; use `:shared:proto`.
- `apps/control-kmp/shared/build.gradle.kts`: Remove `libs.neogenesis.contracts` dependency; use `:shared:proto`.
- `gradle/libs.versions.toml`: Remove `neogenesisContracts` version and `neogenesis-contracts` library alias.
- `gradlew`: Set executable permissions (mode 755).
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/FirmwareGrpcServer.kt`: Enhance `TlsFileWatcher` to monitor `(lastModified, length)` pairs.
- `services/core-server/src/test/kotlin/com/neogenesis/platform/core/GrpcMtlsTest.kt`: Advance `lastModified` by 2000ms and write distinct content for reliable test execution.
- `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModelTest.kt`: Add `CompletableDeferred` synchronization gates to eliminate test race conditions.
- `PLANS.md`: Track CI fix, test stabilization, and verification results.

## Verification Commands
- `./gradlew test --no-daemon`
- `./gradlew :apps:control-kmp:shared:cleanTestReleaseUnitTest :apps:control-kmp:shared:testReleaseUnitTest`
- `gh run view <run-id>`
- `gh pr view 2`

## Rollback Plan
- Revert changes via `git checkout` and restore previous configuration.

## Progress
- [x] Sourced `bioprint.proto` in `shared/proto/src/main/proto/`.
- [x] Removed `libs.neogenesis.contracts` from consuming modules.
- [x] Hardened `TlsFileWatcher` and `GrpcMtlsTest` against filesystem timestamp granularity.
- [x] Hardened `RegenOpsViewModelTest` against asynchronous coroutine races.
- [x] Verified full build and test suite passing locally (`./gradlew test --no-daemon`).
- [x] Updated `PLANS.md`.

# ExecPlan: Repository Cleanup - Remove .patch and Unneeded .md Files (2026-10-15)

## Milestones
1) Identify all `.patch` files and obsolete/unneeded `.md` files across the repository.
2) Remove unneeded `.patch` files from git tracking and disk.
3) Remove obsolete scratch/audit `.md` files from git tracking and disk.
4) Verify repository status, build health, and test suites.

## File-by-File Change List
- `PLANS.md`: Document execution plan, progress, verification commands, and rollback strategy.
- Deletions (.patch files):
  - `Add_backend_complete_Phase_5_integrity_audit_and_prepare_Phase_6_release_resolve_serializa1.patch`
  - `chore__commit_remaining_branch_changes_feat(kmp)__add_billing_UX_gating_and_stabilize_tele.patch`
  - `feat(kmp)__implement_BioKernel_Control_1_0_0_platform_fix(android)__Resolve_Koin_dependenc.patch`
- Deletions (.md files):
  - `AUDIT_REPORT.md` (legacy Windows audit report from early stage development)
  - `FIX_LOG.md` (scratch fix notes from past troubleshooting)
  - `GAP_LIST.md` (legacy gap notes)
  - `README-DEMO.md` (redundant demo note with encoding artifacts, covered in README.md)
  - `RELEASE_PLAN_1.0.0.md` (legacy scratch release plan from /tmp/ environment)
  - `docs/PRODUCTIZATION_PLAN_APP_1.0.0.md` (completed feature implementation plan)
- Retained core files:
  - `AGENTS.md` (agent rules and verification commands)
  - `PLANS.md` (persistent change plan tracking)
  - `README.md` (project overview and monorepo structure)
  - `CHANGELOG.md` (user-visible release notes)
  - `CONTRIBUTING.md` (contributor guidelines)
  - `SECURITY.md` (vulnerability disclosure policy)
  - `.agents/skills/engram-progress/SKILL.md` (agent memory skill)
  - `ops/k8s/README.md` (kubernetes documentation)
  - Core architecture/policy docs in `docs/` (`ARCHITECTURE.md`, `ADR/`, `REGULATORY_TRACEABILITY.md`, `RUNBOOK.md`, `RELEASING.md`, `SECURITY.md`, `ANDROID_BACKUP_POLICY.md`, `BILLING_CLIENT.md`, `OBSERVABILITY_*.md`, `PILOT_INSTALL_CHECKLIST.md`, `qa/UI_ACTIONS_IMPLEMENTED.md`)

## Verification Commands
- `git status`
- `./gradlew test`

## Rollback Plan
- Run `git restore --staged . && git checkout -- .` to restore removed files.
- Re-run verification commands to confirm baseline state.

## Progress
- [x] Prepend ExecPlan to `PLANS.md`.
- [x] Remove `.patch` files.
- [x] Remove unneeded `.md` files.
- [x] Run verification commands and confirm git status.

# ExecPlan: Enterprise Client-Ready Gaps (SEC-05, SRE-04, Android Backup, Audit Pass) (2026-03-01)

## Milestones
1) Discovery: locate current TLS/mTLS, metrics, and Android backup configurations; confirm gaps.
2) EPIC-SEC-05: add gRPC mTLS support + cert rotation hooks and k8s/cert-manager manifests + runbook.
3) EPIC-SRE-04: add business metrics instrumentation + Grafana dashboards + Prometheus rules + metrics contract docs.
4) Android backup hardening: explicit data extraction rules, build-type overrides, and policy doc.
5) Audit pass: probes/compat notes/rollback steps + verification commands.

## File-by-File Change List
- `PLANS.md`: Track this execution plan and progress.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/config/AppConfig.kt`: Add gRPC TLS/mTLS config envs.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/FirmwareGrpcServer.kt`: Add TLS/mTLS config, reload support, and gRPC interceptors.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/observability/Prometheus.kt`: Wire Prometheus registry + metrics adapter.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/observability/Metrics.kt`: Add Prometheus-backed metrics adapter for HTTP/gRPC.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/observability/BusinessMetrics.kt`: Run/evidence business counters + timers.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/observability/GrpcMetrics.kt`: gRPC request metrics.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/GrpcRequestContext.kt`: gRPC label extraction.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/Main.kt`: Wire metrics + mTLS config.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/RegenOpsStore.kt`: Emit business metrics on run lifecycle + gateway events.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/EvidenceModule.kt`: Emit evidence export metrics.
- `services/core-server/src/test/kotlin/com/neogenesis/platform/core/GrpcMtlsTest.kt`: Validate mTLS handshake + reload detection.
- `agents/device-gateway/src/main/kotlin/com/neogenesis/platform/gateway/DeviceGateway.kt`: Support mTLS client certs.
- `ops/k8s/cert-manager/*`: cert-manager + ClusterIssuer + Certificate examples for short-lived mTLS.
- `ops/k8s/core-server/*`: K8s deployment/service/ingress with mTLS secret wiring + probes.
- `ops/k8s/device-gateway/*`: Example gateway deployment with client certs.
- `ops/prometheus/rules/regenops-business.rules.yaml`: Recording rules for dashboards.
- `ops/grafana/dashboards/regenops-business.json`: Grafana business dashboard.
- `docs/RUNBOOK.md`: mTLS rotation runbook + rollback steps.
- `docs/OBSERVABILITY_METRICS_CONTRACT.md`: Metrics labels contract (no PHI).
- `docs/OBSERVABILITY_DASHBOARDS.md`: Grafana import + label expectations.
- `docs/SECURITY.md`: mTLS boundary notes.
- `docs/ARCHITECTURE.md`: gRPC compatibility note.
- `apps/control-kmp/androidApp/src/main/res/xml/data_extraction_rules.xml`: Explicit include/exclude backup rules.
- `apps/control-kmp/androidApp/src/main/res/xml/backup_rules.xml`: Pre-Android 12 backup rules.
- `apps/control-kmp/androidApp/src/main/AndroidManifest.xml`: Wire data extraction rules for release.
- `apps/control-kmp/androidApp/src/debug/AndroidManifest.xml`: Allow backups in debug only.
- `docs/ANDROID_BACKUP_POLICY.md`: Backup policy + MDM override guidance.
- `docs/RELEASING.md`: Update release checklist if needed for new items.
- `gradle/libs.versions.toml`: Add test dependency versions.
- `apps/control-kmp/shared/build.gradle.kts`: Add missing test dependencies.
- `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModelTest.kt`: Align to current API (placeholder).

## Verification Commands
- `./gradlew.bat test`
- `./gradlew.bat :services:core-server:compileKotlin`

## Rollback Plan
- Revert the files listed above.
- Remove new k8s/observability directories if they are not desired.
- Re-run the verification commands to confirm baseline stability.

## Progress
- [x] Discovery and gap validation.
- [x] mTLS rotation with cert-manager manifests + gRPC reload hooks + tests.
- [x] Business metrics + Prometheus rules + Grafana dashboards + metrics contract docs.
- [x] Android backup hardening (rules + manifest overrides + policy doc).
- [x] Verification: `./gradlew.bat test` and `./gradlew.bat :services:core-server:compileKotlin`.
# ExecPlan: RegenOps Control App + Core Server Demo Enablement (2026-03-01)

## Milestones
1) Fix UI bugs: simulate chip, dialog alignment, start mission robustness.
2) Wire simulation API end-to-end with demo tenant + correlation id support.
3) Make all UI actions functional across screens; document in UI action audit.
4) Add server demo-gated endpoints for metrics, commercial pipeline, and exports alignment.
5) Add client/server tests to cover new flows.

## File-by-File Change List
Client
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/design/NgComponents.kt`: Make status chip optionally clickable with role and hover.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt`: Wire SIMULATE/SIMULATION chip, fix dialogs alignment, start mission UI state.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt`: Remove simulation gate, add start/run loading and errors, wire simulator flow.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/SimulatorApi.kt`: New simulator API client.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/HttpSimulatorApi.kt`: HTTP implementation for simulator.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/HttpClientFactory.kt`: Add tenant_id + X-Correlation-Id support.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/AppConfig.kt`: Add demo tenant default if needed.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/ExportsApi.kt`: Align export endpoints or client paths.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/TraceApi.kt`: Ensure endpoints match server.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/CommercialApi.kt`: Ensure endpoints match server.
- `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModelTest.kt`: Add start/run tests.
- `apps/control-kmp/androidApp/src/androidTest/kotlin/...`: Compose UI tests for chip, dialogs, and start flows.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/design/NgPointer.kt`: Expect pointer hover helper.
- `apps/control-kmp/shared/src/androidMain/kotlin/com/neogenesis/platform/control/presentation/design/NgPointer.kt`: Android no-op hover helper.
- `apps/control-kmp/shared/src/jvmMain/kotlin/com/neogenesis/platform/control/presentation/design/NgPointer.kt`: Desktop hover pointer icon.
- `apps/control-kmp/shared/src/androidMain/kotlin/com/neogenesis/platform/control/data/remote/GrpcControlApi.kt`: Stub listRuns for platform builds.
- `apps/control-kmp/shared/src/jvmMain/kotlin/com/neogenesis/platform/control/data/remote/GrpcControlApi.kt`: Stub listRuns for platform builds.
- `docs/qa/UI_ACTIONS_IMPLEMENTED.md`: UI action audit and endpoint mapping.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/TelemetryModule.kt`: Respect `Accept: text/csv` for export.
- `apps/control-kmp/androidApp/src/main/java/com/neogenesis/platform/control/android/MainActivity.kt`: Map localhost baseUrl to emulator host when needed.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt`: Fix version dialog item alignment.

Server
- `src/main/kotlin/com/neogenesis/server/config/AppConfig.kt`: Add NG_DEMO_MODE flag (if config lives here).
- `src/main/kotlin/com/neogenesis/server/modules/demo/SimulatorModule.kt`: Ensure demo gating and role checks.
- `src/main/kotlin/com/neogenesis/server/modules/metrics/MetricsModule.kt`: Add reproducibility-score + drift-alerts endpoints.
- `src/main/kotlin/com/neogenesis/server/modules/commercial/CommercialModule.kt`: Add pipeline + export endpoints.
- `src/main/kotlin/com/neogenesis/server/modules/exports/ExportsModule.kt`: Add export aliases if needed.
- `src/test/kotlin/com/neogenesis/server/...`: Integration tests for demo endpoints and simulator.

## Verification Commands
- `./gradlew.bat test`
- `./gradlew.bat :backend:test`
- `./gradlew.bat :domain:test`
- `./gradlew.bat :shared-network:test`
- `./gradlew.bat :androidApp:assembleDebug`

## Rollback Plan
- Revert the files listed above.
- Remove demo-only modules/endpoints and keep production behavior unchanged.
- Re-run the verification commands to confirm baseline stability.

## Progress
- [x] Milestone 1: Fix UI bugs (chip, dialogs, start mission).
- [x] Milestone 2: Simulation API end-to-end (tenant + correlation).
- [x] Milestone 3: UI action audit + no-op removals.
- [x] Milestone 4: Demo-gated server endpoints (metrics, commercial, exports).
- [x] Milestone 5: Tests + docs (added coverage; local runs complete).

# ExecPlan: Protocol UX + Demo Server Fixes + Extra Polish (2026-03-01)

## Milestones
1) Fix demo server compile issues (duplicate fields, missing request DTOs) and ensure protocol status update endpoint works.
2) Add missing protocol fields in mock/demo data and align UI bindings.
3) Enhance Protocol list and detail UI polish (folder card, timeline, evidence, animations).
4) Add create protocol dialog (full-screen-ish) wired to server create endpoint.
5) Verify build/test commands (targeted) and document run instructions.

## File-by-File Change List
Client
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt`: Protocol list card polish and create protocol dialog.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/screens/ProtocolsScreen.kt`: Folder card expand/collapse + evidence timeline panels.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/screens/ProtocolDetailScreen.kt`: Status updates + evidence actions + timeline.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/design/NgComponents.kt`: Chips/buttons and timeline visuals.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/MockProtocols.kt`: Add missing demo fields (status, lastRunId, evidenceArtifacts).
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/remote/ControlApi.kt`: Add create + status update calls.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt`: Status transition logic, errors, and create protocol flow.

Server
- `src/main/kotlin/com/neogenesis/server/modules/demo/DemoUiModule.kt`: Fix duplicate `status`, add/update request DTOs, and ensure status update endpoint works.
- `src/main/kotlin/com/neogenesis/server/modules/demo/DemoProtocolStore.kt`: Demo protocol fields and deterministic data.

## Verification Commands
- `./gradlew.bat :services:core-server:compileKotlin`
- `./gradlew.bat :apps:control-kmp:shared:test`

## Rollback Plan
- Revert the files listed above.
- Re-run the verification commands to confirm baseline stability.

## Progress
- [x] Milestone 1: Demo server compile fixes.
- [ ] Milestone 2: Demo protocol data alignment.
- [ ] Milestone 3: UI polish completion.
- [ ] Milestone 4: Create protocol dialog end-to-end.
- [ ] Milestone 5: Verification.

# ExecPlan: Device Support Tiers v1.0.0 (BioKernel + NeoGenesis-Core-Server) (2026-03-01)

## Milestones
1) Define shared device-tier domain model + policy math + tests (BioKernel), and mirror model in NeoGenesis.
2) Add policy transport (proto + HTTP DTOs) and device header/metadata wiring for clients.
3) Implement client detection + policy registration + capability gating + device-gateway identification (BioKernel).
4) Enforce server-side tier capability checks for HTTP + gRPC in both servers with explicit mappings.
5) Add integration tests (HTTP + gRPC where available) for Tier1/Tier2/Tier3 enforcement.

## File-by-File Change List
BioKernel
- `shared/domain/src/commonMain/kotlin/com/neogenesis/platform/shared/domain/device/DevicePolicyModels.kt`: Device tiers/classes/capabilities + DeviceInfo/DevicePolicy.
- `shared/domain/src/commonMain/kotlin/com/neogenesis/platform/shared/domain/device/DevicePolicyLogic.kt`: defaultCapabilitiesFor + effectiveCapabilities.
- `shared/domain/src/commonTest/kotlin/com/neogenesis/platform/shared/domain/device/DevicePolicyLogicTest.kt`: Tier/capability tests.
- `shared/proto/src/main/proto/device_policy.proto`: DevicePolicyService + DeviceInfo/DevicePolicy messages.
- `shared/network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/DeviceHeaders.kt`: Header names + mapping helpers.
- `shared/network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/HttpClientFactory.kt`: Attach device headers to all requests.
- `shared/network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/NetworkConfig.kt`: Add deviceInfoProvider.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/device/DeviceDetection.kt`: expect/actual device detection.
- `apps/control-kmp/shared/src/androidMain/kotlin/com/neogenesis/platform/control/device/DeviceDetection.android.kt`: Android phone/tablet detection.
- `apps/control-kmp/shared/src/jvmMain/kotlin/com/neogenesis/platform/control/device/DeviceDetection.jvm.kt`: Desktop detection + overrides.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/device/CapabilityGate.kt`: Capability gating utility + unsupported screen routing.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/DevicePolicyApi.kt`: HTTP register + policy fetch.
- `apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/data/AppState.kt`: Store device policy + effective caps.
- `apps/control-kmp/androidApp/src/main/AndroidManifest.xml`: Ensure device info sources available if needed.
- `apps/control-kmp/desktopApp/src/jvmMain/kotlin/com/neogenesis/platform/control/desktop/DesktopApp.kt`: Load overrides + register device policy on startup.
- `shared/network/src/commonMain/kotlin/com/neogenesis/platform/shared/network/grpc/GrpcDeviceHeaders.kt`: gRPC metadata interceptor.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/device/DevicePolicyRepository.kt`: Policy loading.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/device/DeviceContext.kt`: Parse device headers, compute effective caps.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/security/DeviceCapabilityGuard.kt`: requireCapability for HTTP + gRPC.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/DevicePolicyModule.kt`: HTTP endpoints for policy/register.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/DevicePolicyGrpcService.kt`: gRPC DevicePolicyService.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/GrpcRequestContext.kt`: Add device info metadata.
- `services/core-server/src/main/resources/device-policy.yaml`: Default policy config.
- `services/core-server/src/test/kotlin/com/neogenesis/platform/core/DeviceTierHttpEnforcementTest.kt`: HTTP enforcement tests.
- `services/core-server/src/test/kotlin/com/neogenesis/platform/core/DeviceTierGrpcEnforcementTest.kt`: gRPC enforcement tests.
- `agents/device-gateway/src/main/kotlin/com/neogenesis/platform/gateway/DeviceGateway.kt`: Send device headers + TIER_1 EMBEDDED.

NeoGenesis-Core-Server
- `src/main/kotlin/com/neogenesis/server/domain/device/DevicePolicyModels.kt`: Device tiers/classes/caps + DeviceInfo/DevicePolicy.
- `src/main/kotlin/com/neogenesis/server/domain/device/DevicePolicyLogic.kt`: defaultCapabilitiesFor + effectiveCapabilities.
- `src/main/kotlin/com/neogenesis/server/infrastructure/device/DevicePolicyRepository.kt`: Policy loading.
- `src/main/kotlin/com/neogenesis/server/infrastructure/device/DeviceContext.kt`: Parse device headers, compute effective caps.
- `src/main/kotlin/com/neogenesis/server/infrastructure/security/DeviceCapabilityGuard.kt`: requireCapability + audit logging.
- `src/main/kotlin/com/neogenesis/server/presentation/http/DevicePolicyRoutes.kt`: HTTP GET/POST routes.
- `src/main/resources/device-policy.yaml`: Default policy config.
- `src/test/kotlin/com/neogenesis/server/DeviceTierHttpEnforcementTest.kt`: HTTP enforcement tests.
- `src/test/kotlin/com/neogenesis/server/DeviceTierGrpcEnforcementTest.kt`: gRPC enforcement tests if gRPC exists.

## Verification Commands
- `./gradlew.bat test`
- `./gradlew.bat :backend:test`
- `./gradlew.bat :domain:test`
- `./gradlew.bat :shared-network:test`
- `./gradlew.bat :desktopApp:run`
- `./gradlew.bat :androidApp:assembleDebug`

## Rollback Plan
- Revert the files listed above in both repos.
- Remove `device-policy.yaml` if unused and restore prior routing/guards.
- Re-run verification commands to confirm baseline behavior.

## Progress
- [x] Milestone 1: Device-tier domain model + tests.
- [x] Milestone 2: Transport headers/proto/DTOs wired.
- [x] Milestone 3: Client detection + gating + device-gateway updates.
- [x] Milestone 4: Server enforcement (HTTP + gRPC).
- [x] Milestone 5: Integration tests.

# ExecPlan: Phase 4 Finish + Release 1.0.0 (BioKernel + NeoGenesis-Core-Server) (2026-03-02)

## Milestones
1) Fix remaining test failures in BioKernel.
2) Run full test suites in both repos and verify green.
3) Update release artifacts (CHANGELOG/VERSION if needed) and commit/push with required timestamps.

## File-by-File Change List
BioKernel
- `apps/control-kmp/shared/src/commonTest/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModelTest.kt`: Fix test timeout under Dispatchers.Default.

NeoGenesis-Core-Server
- (TBD) Only if tests fail and require fixes.

## Verification Commands
- `./gradlew.bat test`

## Rollback Plan
- Revert the files listed above.
- Re-run `./gradlew.bat test` to confirm baseline.

## Progress
- [x] Milestone 1: Fix remaining test failures in BioKernel.
- [x] Milestone 2: Run full test suites in both repos and verify green.
- [x] Milestone 3: Update release artifacts and commit/push with required timestamps.

# ExecPlan: Phase 4 Integrated MVP + Production Hardening (BioKernel + NeoGenesis-Core-Server) (2026-03-02)

## Milestones
1) Inventory: full HTTP + gRPC endpoint mapping tables in both repos; verify default-deny for mutating endpoints.
2) Auditability: persistent audit event model + sink; audit success/deny for control/protocol/admin + policy changes.
3) Reliability: enforce correlation IDs, timeouts, retries/backpressure, and safe reconnect semantics.
4) Observability/Ops: structured logs, metrics for requests/denies/streams/gateway health, liveness/readiness, runbook docs.
5) CI + Release: tests for device policy routes + tier enforcement, CI tasks, update CHANGELOG + version 1.0.0, pilot checklist.

## File-by-File Change List
BioKernel
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/security/DeviceCapabilityGuard.kt`: default-deny handling + audit details.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/audit/*`: audit event model + repository (if missing).
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/*`: ensure requireCapability for all mapped endpoints.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/grpc/*`: enforce capability mapping for all RPCs.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/observability/*`: metrics + structured logging.
- `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/HealthModule.kt`: liveness/readiness.
- `services/core-server/src/main/resources/device-policy.yaml`: verify policy config.
- `services/core-server/src/test/kotlin/com/neogenesis/platform/core/*`: add device policy route + tier enforcement tests.
- `docs/*`: runbook + pilot install checklist updates.
- `CHANGELOG.md` and `VERSION`: bump to 1.0.0.

NeoGenesis-Core-Server
- `src/main/kotlin/com/neogenesis/server/infrastructure/security/DeviceCapabilityGuard.kt`: default-deny + audit details.
- `src/main/kotlin/com/neogenesis/server/modules/*` + `src/main/kotlin/com/neogenesis/server/infrastructure/grpc/*`: ensure requireCapability for all mapped endpoints.
- `src/main/kotlin/com/neogenesis/server/observability/*` or existing metrics/logging modules: add metrics + structured logs.
- `src/main/kotlin/com/neogenesis/server/modules/HealthModule.kt`: liveness/readiness.
- `src/main/resources/device-policy.yaml`: verify policy config.
- `src/test/kotlin/com/neogenesis/server/*`: device policy route + tier enforcement tests.
- `docs/*`: runbook + pilot install checklist updates.
- `CHANGELOG.md` and version config: bump to 1.0.0.

## Verification Commands
- `./gradlew.bat test`
- `./gradlew.bat :services:core-server:test`
- `./gradlew.bat :backend:test`
- `./gradlew.bat :domain:test`
- `./gradlew.bat :shared-network:test`
- `./gradlew.bat :androidApp:assembleDebug`

## Rollback Plan
- Revert the files listed above in both repos.
- Remove added docs/metrics/audit modules if necessary.
- Re-run verification commands to confirm baseline stability.

## Progress
- [x] Milestone 1: Inventory and mapping.
- [x] Milestone 2: Auditability.
- [x] Milestone 3: Reliability.
- [x] Milestone 4: Observability/Ops.
- [x] Milestone 5: CI + Release.

# ExecPlan: Phase 4 Commit + Push (BioKernel) (2026-03-02)

## Milestones
1) Review working tree and stage Phase 4 changes for core server + KMP app.
2) Commit with Phase 4 message.
3) Push to the current branch.

## File-by-File Change List
- `PLANS.md`: Track this execution plan.
- (TBD after status) Capture the concrete Phase 4 files to be committed.

## Verification Commands
- None (commit/push only).

## Rollback Plan
- `git reset --soft HEAD~1` if the commit needs to be amended.
- `git push --force-with-lease` only if the remote commit must be replaced.

## Progress
- [x] Milestone 1: Review and stage.
- [x] Milestone 2: Commit.
- [x] Milestone 3: Push.
