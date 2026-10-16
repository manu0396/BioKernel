# Research: RegenOps Control App MVP

**Date**: 2026-10-16
**Scope**: Resolve planning decisions from the MVP specification using repository documentation and source structure. This is repository research, not an external product or device certification.

## Decision 1: Treat RegenOps Control as the main app

**Decision**: Plan the Android and desktop Control app operator workflow, not the retinal-analysis domain models found elsewhere in the repository.

**Rationale**: The root README describes the Control KMP app. `docs/qa/UI_ACTIONS_IMPLEMENTED.md` documents protocols, run control, live run, exports, trace, and authorization. The active feature specification explicitly confirmed this product scope.

**Alternatives considered**:
- Retinal sample analysis: not selected; it is not the user-facing workflow described by the primary product documentation.
- Combine both products in one MVP: rejected because that would broaden the independently testable pilot and mix distinct workflows.

## Decision 2: Reuse the existing module layout and contracts

**Decision**: Keep changes within `apps/control-kmp`, `services/core-server`, `agents/device-gateway`, and shared protocol/data modules as necessary. Prefer compatible fixes over new modules or services.

**Rationale**: `settings.gradle.kts` defines the application, backend, gateway, and shared Gradle projects. The architecture ADR chooses Kotlin Multiplatform with a Ktor backend and gRPC streaming. The Constitution requires module boundaries, behavior tests, and compatibility.

**Alternatives considered**:
- Rebuild the backend or replace the transport: rejected absent evidence that existing contracts cannot meet an acceptance requirement.
- Add an MVP-specific app/service: rejected because the existing Control app and service already contain relevant workflows.

## Decision 3: Model both app HTTP control and device streaming paths

**Decision**: Treat Control-to-core REST calls and gateway/device gRPC streams as separate interfaces whose run identity and lifecycle mapping must be proven end to end.

**Rationale**: `RegenOpsHttpModule` exposes protocol listing, run listing/start, pause, abort, and publish. `ExportsAliasModule` exposes CSV report and evidence ZIP routes. Protobuf files define platform run/telemetry services and a separate firmware bridge. The HTTP run route calls `RegenOpsInMemoryStore`; the source and documentation reviewed do not establish that this route alone initiates a physical firmware run. Planning must verify the complete path before accepting the connected run.

**Alternatives considered**:
- Treat a successful HTTP run-start response as proof of a physical run: rejected; no such end-to-end equivalence is documented.
- Modify protocol definitions first: rejected; inspect and test current contracts before introducing changes.

## Decision 4: Retain OIDC device authorization as the app identity baseline, subject to integration verification

**Decision**: Phase 0 verifies the documented OIDC device flow, issuer/client/audience configuration, token lifecycle, and backend authorization mapping before implementation work relies on it.

**Rationale**: The README describes OIDC device authorization and the shared app contains OIDC device-auth services. The backend also exposes username/password routes in `AuthModule`; these appear to be separate authentication surfaces and need an explicit client-to-service mapping.

**Alternatives considered**:
- Use the backend username/password endpoint by default: rejected because the app documentation states OIDC device flow.
- Replace the identity provider: out of scope unless Phase 0 shows the documented flow is unusable for the pilot.

## Decision 5: Require one representative physical device for connected acceptance

**Decision**: Validate both simulated and real connected-device runs on the documented Android and desktop targets, with one supported physical device and gateway as the v1.0.0 minimum. Record the tested configuration; do not imply fleet-wide compatibility.

**Rationale**: This is the product-owner clarification recorded in the spec. The pilot checklist and architecture describe a device gateway, policy enforcement, and gRPC connection, but no selected physical model is recorded in the reviewed docs.

**Alternatives considered**:
- Simulator-only: rejected by the clarified acceptance requirement.
- Every supported device model/tier: rejected as beyond the agreed controlled pilot scope.

## Decision 6: Treat v1.0.0 as a release target that needs metadata reconciliation

**Decision**: Do not alter release metadata in planning. Implementation tasks must reconcile actual root, Android, desktop, `VERSION`, and changelog values and verify exact Gradle task paths before tagging.

**Rationale**: Root Gradle and desktop metadata report `1.0.0`, Android `versionName` reports `0.1.0`, while README/release docs contain historical or stale module paths and commands. Existing labels do not demonstrate that the clarified MVP gates passed.

**Alternatives considered**:
- Assume the repo is already v1.0.0-ready: rejected because version metadata conflicts and release acceptance evidence is not established.

## Decision 7: Define telemetry freshness during readiness discovery

**Decision**: Before writing telemetry acceptance tests, measure/confirm the selected device's expected cadence and agree a stale-data threshold with the pilot owner. Until then, tests assert the distinction between fresh, delayed, and disconnected states without inventing a numeric SLA.

**Rationale**: The spec requires stale data to be identified, and the README describes streaming with backoff, but the project documentation gives no Control telemetry freshness target.

**Alternatives considered**:
- Invent a fixed latency target in the plan: rejected because the device cadence and pilot expectation are unknown.
- Treat delivery of a stream frame as proof of freshness: rejected because it does not define user-visible staleness.

## Decision 8: Use root CI and current Gradle project paths as the baseline

**Decision**: Use `./gradlew test --no-daemon` as the repository CI baseline, then select platform/service-specific test tasks after verifying them against the current project paths. Distinguish emulator, desktop, and connected-hardware evidence.

**Rationale**: `.github/workflows/ci.yml` runs the root test task. Actual project paths are nested under `:apps:control-kmp:*`, `:services:core-server`, and `:shared:*`; some `CONTRIBUTING.md` and `docs/RELEASING.md` examples reference older paths.

**Alternatives considered**:
- Copy all legacy commands verbatim: rejected because they do not match current settings.gradle project names.

## Decision 9: Reconcile endpoint and Compose environment configuration

**Decision**: Make the pilot's HTTP/gRPC endpoint values and Compose environment
file resolution consistent before connected-run testing. Keep one canonical
root environment file and configure the Compose service env-file path to resolve
to it, or document another explicit, tested single-source arrangement.

**Rationale**: `.env.example` sets the service and Control gRPC port to `9090`,
but Android's build configuration defaults its gRPC port to `50051`. In
`ops/docker-compose.yml`, interpolation reads the shell/CLI environment while
the service `env_file: .env` is resolved relative to the Compose file's
directory. The repository instructions do not explain this difference.

**Alternatives considered**:
- Rely on implicit Android defaults: rejected because they differ from the
  documented environment.
- Maintain undocumented divergent root and `ops/` environment files: rejected
  because it can make a local pilot connect to the wrong endpoint or omit
  service configuration.

## Readiness Items to Resolve Before Implementation

1. Name and provision the physical device, firmware, gateway build/configuration, and test environment.
2. Verify the OIDC authority and how app tokens map to core-service roles/device capabilities.
3. Demonstrate the actual app-start to gateway command to device telemetry to active-app run path, including run/job ID mapping.
4. Reconcile gRPC host/port values, verify the Compose env-file path, and verify exact Android/desktop build/test task names from the repository root.
5. Agree and record telemetry cadence/freshness acceptance. Confirm whether accessibility or localization has any pilot-specific mandatory requirement.

These are implementation-readiness gates, not requests to expand the MVP scope. No production code change is authorized by this planning artifact.
