# AUDIT_REPORT.md

## Scope & Environment
- Repo root: C:\Users\manul\AndroidStudioProjects\BioKernel
- Audit role: KMP + Compose Multiplatform engineer and release auditor
- Tooling: Gradle 8.11.1, Kotlin 2.0.20, JDK 21.0.10 (from `./gradlew --version`)

## Module / Target Map
- Modules (from `settings.gradle.kts`): `:domain`, `:data`, `:shared-network`, `:androidApp`, `:desktopApp`, `:backend`, `:firmware-protocol`
- KMP targets:
  - `:domain`: `androidTarget`, `jvm("desktop")`
  - `:data`: `androidTarget`, `jvm("desktop")`
  - `:shared-network`: `androidTarget`, `jvm("desktop")`
- UI stack:
  - Android: Compose (Material 3)
  - Desktop: Compose Multiplatform
- Networking:
  - Client: Ktor client (`okhttp` on Android, `cio` on Desktop)
  - Desktop gRPC client (grpc-kotlin + netty)
- Backend:
  - Ktor server + gRPC server, Exposed + Flyway + Postgres
- DI: No DI framework in client (manual wiring); Koin in backend
- Persistence:
  - Client: Token storage (Android EncryptedSharedPreferences; Desktop Properties file)
  - Backend: Postgres via Exposed, Flyway migrations

## Architecture Snapshot
- `:domain`: shared models, evidence hashing, telemetry models, validation, use cases
- `:data`: DTOs and basic serialization mapping
- `:shared-network`: Ktor APIs (`AuthApi`, `RecipeApi`, `PrintJobApi`), HTTP client factory, token storage interface
- `:androidApp`: Compose UI for auth, recipes, print jobs, telemetry export (HTTP)
- `:desktopApp`: Compose UI for auth, recipes, print jobs, telemetry stream (gRPC)
- `:backend`: Ktor routes + gRPC services, audit trail, evidence export, repositories
- `:firmware-protocol`: protobuf/gRPC stubs
- Data flow:
  - UI -> `shared-network` API -> backend HTTP routes
  - Telemetry: desktop subscribes to gRPC stream; Android pulls HTTP telemetry export snapshot
  - Audit trail is server-side hash chain; evidence packages produced by backend

## Build / Test Verification
Commands executed (latest pass):
- `./gradlew --version` -> OK (Gradle 8.11.1, Kotlin 2.0.20, JDK 21.0.10)
- `./gradlew clean build --stacktrace` -> OK
- `./gradlew test --stacktrace` -> OK
- `./gradlew lint --stacktrace` -> OK
- `./gradlew detekt ktlintCheck --stacktrace` -> OK (ktlint warnings reported, but tasks pass)

Notes:
- Added `kotlin.daemon.jvmargs` and increased Gradle heap to stabilize builds.
- Android release signing check is now gated behind `RELEASE_SIGNING_REQUIRED=true` and defaults to debug signing for local builds.
- Added Proguard `-dontwarn` rules for errorprone annotations and slf4j binder to prevent R8 missing-class errors.

## Required Scope Checklist (Client)
Legend: ? implemented, ?? partial/needs work, ? missing

1) Auth/session
- ? Token storage (Android EncryptedSharedPreferences, Desktop Properties)
- ?? Refresh/expiry handling (no automatic refresh or expiry checks)
- ?? Environment config (manual base URL entry; no dev/stage/prod profiles)

2) Device registry
- ? UI listing/pairing status in client
- ?? Backend supports devices/pairing, but client UI lacks flows

3) Telemetry streaming
- ? Desktop gRPC streaming (manual start/stop)
- ?? Android uses HTTP export (no real-time stream)
- ?? No reconnection/backoff policy; minimal buffering
- ?? No explicit stale/connected/disconnected UI states

4) Commands/control
- ?? Print job create + status update via HTTP
- ? Acknowledgements, timeouts, idempotency keys not surfaced in client

5) Recipes
- ? Create + activate + list
- ?? No explicit parameter validation or versioning exposed in UI

6) Audit trail (client-side events)
- ? No client-side audit event hashing/chain

7) Evidence export
- ?? Backend produces evidence package; client has no UI export workflow

8) Observability
- ?? Server has structured logging; client logging/correlation ids not explicit

9) Offline tolerance
- ? No cached device list or telemetry snapshot; no queued actions

## gRPC Client Validation
- Protobuf/gRPC generation succeeds in `:firmware-protocol`.
- Desktop uses coroutine stub (`TelemetryStreamServiceGrpcKt`) with manual cancellation; no retry/backoff.
- Android does not use gRPC; HTTP export used for telemetry snapshots.

## Security / Correctness Quick Pass
- ? No hardcoded secrets detected in the audited client paths.
- ?? Cleartext transport allowed in client (`allowCleartext = true`); needs gated prod config.
- ?? No token refresh/expiry handling; logout clears tokens.
- ? TLS is supported at backend (see config), but client currently defaults to HTTP.

## Summary
- Builds/tests/lint pass locally after stabilization.
- Client implements auth, recipes, print jobs, and basic telemetry viewing (desktop gRPC / Android HTTP export).
- Major gaps remain for device registry UI, offline tolerance, audit trail in client, and production-grade telemetry resiliency.