# NeoGenesis Platform (RegenOps)
Version: 1.0.0

Monorepo for RegenOps: Control (KMP), Core (Ktor + gRPC), Device Gateway, and shared libraries.

The current `1.0.0` Android artifact is a mock-backed demo/test build, not a
controlled-pilot release. Synthetic OIDC and gateway behavior is confined to
automated tests; the demo app remains unauthenticated and labels synthetic
runs. Real identity-provider, lab device/gateway, telemetry freshness, and
operator-usability acceptance are still required for pilot approval.

## Monorepo Layout
- `apps/control-kmp`: KMP Control app (Android/Desktop).
- `services/core-server`: Core server (Ktor + gRPC, modular monolith).
- `agents/device-gateway`: Edge gateway agent (gRPC client).
- `shared/proto`: Protobuf contracts (gRPC stubs for server/gateway/client).
- `shared/domain`: Shared domain models and utilities.
- `shared/data`: API DTOs.
- `shared/network`: Shared Ktor clients/utilities.
- `ops`: Docker Compose, Makefile, scripts.

## Repo Tree (Top-Level)
- `agents/`
- `apps/`
- `shared/`
- `services/`
- `ops/`
- `docs/`

## Runtime Ports
- HTTP API: `HTTP_PORT` (default `8080`)
- gRPC Core: `GRPC_PORT` (default `9090`)

## Environment Variables
Core:
- `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_DRIVER`, `DB_POOL_SIZE`
- `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_SECRET`, `JWT_ACCESS_MS`, `JWT_REFRESH_MS`
- `PAIRING_SECRET`
- `HTTP_PORT`, `GRPC_PORT`, `GRPC_ENABLED`

Control (KMP):
- `REGENOPS_HTTP_BASE_URL`
- `REGENOPS_GRPC_HOST`, `REGENOPS_GRPC_PORT`, `REGENOPS_GRPC_TLS`
- `OIDC_ISSUER`, `OIDC_CLIENT_ID`, `OIDC_AUDIENCE`
- Android Gradle builds read these from repository-root `.env`, `local.properties`,
  Gradle project properties (`-POIDC_CLIENT_ID=...`), or the Gradle process
  environment. Explicit local/Gradle/environment values take precedence over
  `.env`; `.env` is not checked in.
- Desktop token store: `REGENOPS_TOKENSTORE_PASSWORD`
  - Set `REGENOPS_GRPC_HOST` to empty to force HTTP fallback.
- Optional commercial screen: `COMMERCIAL_MODE=true`
- Optional founder exports screen: `FOUNDER_MODE=true`
- Android emulator host mapping: use `REGENOPS_HTTP_BASE_URL=http://10.0.2.2:8080`
  - Physical device: use your host LAN IP, e.g. `http://192.168.1.10:8080`
- Demo env file: `apps/control-kmp/demo.env`

Gateway:
- `CORE_GRPC_HOST`, `CORE_GRPC_PORT`, `CORE_GRPC_TLS`
- `GATEWAY_ID`
- `SIM_RUN_ID`

## Run Local (Walking Skeleton)
1. Copy `.env.example` to `.env` and set secrets.
2. Start core server + Postgres:
   - `docker-compose -f ops/docker-compose.yml up --build`
   - Windows: `powershell -ExecutionPolicy Bypass -File ops/runLocal.ps1`
3. Start gateway:
   - `./gradlew :agents:device-gateway:run`
4. Start Control app:
   - Desktop: `./gradlew :apps:control-kmp:desktopApp:run`
   - Android: `./gradlew :apps:control-kmp:androidApp:assembleDebug`

## Mock-backed Demo/Test Build

Build the explicitly labeled Android demo variant with:

```bash
./gradlew :apps:control-kmp:androidApp:assembleDemoGoogleDebug -PDEMO_MODE=true --rerun-tasks
```

For Android authentication, set `OIDC_ISSUER` and `OIDC_CLIENT_ID` to values
registered with your OIDC provider in the repository-root `.env` (or in
`local.properties`/Gradle properties). Then sync/rebuild the Android project;
the values are embedded in that build's `BuildConfig`, so installing an older
APK does not pick up changed values. Example local-only `.env` entries:

```dotenv
OIDC_ISSUER=https://your-identity-provider.example/
OIDC_CLIENT_ID=your-registered-public-client-id
OIDC_AUDIENCE=
```

The example values above are placeholders, not working credentials. For a
one-off command-line build, use
`-POIDC_ISSUER=https://... -POIDC_CLIENT_ID=...`; avoid putting sensitive
values in shared shell history or committing `.env`/`local.properties`.
**OIDC is currently unconfigured in this checkout:** `.env.example` leaves
issuer and client ID blank, so authentication will report missing configuration
until valid provider values are supplied and the app is rebuilt. The demo
session does not authenticate users.

Run deterministic identity, gateway, and service-stream tests with:

```bash
./gradlew :apps:control-kmp:shared:jvmTest --tests '*OidcRepositoryTest' \
  :agents:device-gateway:test :services:core-server:test
```

These test doubles do not authenticate users or represent real device
commands/telemetry. For controlled-pilot criteria and remaining gates, see
`specs/001-control-app-mvp/readiness-matrix.md`.

## DevEx
- `make -f ops/Makefile runLocal`
- `make -f ops/Makefile test`
- `make -f ops/Makefile lint`
- `make -f ops/Makefile format`

## Observability
- JSON logs (core-server logback)
- Prometheus metrics endpoint (to be wired in core-server)
- OTEL tracing (baseline planned)

## Notes
- OIDC device flow is used for Control auth.
- mTLS for gateway<->core is optional; plaintext allowed in dev via env vars.
- HTTP fallback endpoints (for iOS compatibility planning):
  - `GET /api/v1/regenops/protocols`
  - `POST /api/v1/regenops/runs/start`
- Export endpoints (founder mode):
  - `GET /api/v1/telemetry/{runId}/export`
  - `GET /api/v1/evidence/{runId}/package`

## Troubleshooting
- Streaming resumes automatically with backoff; if telemetry stalls, check gRPC connectivity and gateway heartbeat.
- If auth fails, verify `OIDC_ISSUER` and `OIDC_CLIENT_ID` are set.
- Logs are JSON; capture with `docker logs regenops-core-server` or app logcat.
