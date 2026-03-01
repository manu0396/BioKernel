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
