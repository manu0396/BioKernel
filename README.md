# NeoGenesis Platform (RegenOps)
Version: 1.0.0

Monorepo for RegenOps: Control (KMP), Core (Ktor + gRPC), Device Gateway, and shared libraries.

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
- Desktop token store: `REGENOPS_TOKENSTORE_PASSWORD`
  - Set `REGENOPS_GRPC_HOST` to empty to force HTTP fallback.

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
