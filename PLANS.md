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
