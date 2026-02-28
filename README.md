# NeoGenesis Platform
Version: 1.0.0

Industrial-grade Kotlin Multiplatform system for closed-loop micrometric bioprinting. Designed for ISO 13485 traceability and hospital deployment.

## Modules
- `domain`: Domain models, use cases, telemetry, digital twin core, evidence logging, validation, errors, security primitives.
- `data`: API DTOs and explicit mappers.
- `shared-network`: KMP HttpClient factory, retry/timeout policy, typed API layer, sealed error mapping.
- `androidApp`: Compose Multiplatform Android UI.
- `desktopApp`: Compose Multiplatform Desktop UI.
- `backend`: Ktor + gRPC backend, PostgreSQL (Exposed), JWT auth, RBAC, audit trail.
- `firmware-protocol`: Protobuf + gRPC definitions for STM32 communication.

## Architecture
- Clean architecture with strict separation of domain (`domain`) and delivery (`androidApp`, `desktopApp`, `backend`).
- Deterministic Digital Twin engine in `domain` for reproducible simulations.
- Evidence suite uses hash-chained audit events for immutability.
- Backend enforces JWT auth + RBAC, structured logging, and PostgreSQL persistence.
- gRPC bidirectional streaming for device telemetry and command control.

## Runtime Ports
- HTTP API: `HTTP_PORT` (default `8080`)
- gRPC Device Bridge: `GRPC_PORT` (default `9090`)

## Environment Variables
- `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `DB_DRIVER` (optional), `DB_POOL_SIZE` (optional)
- `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_SECRET`
- `JWT_ACCESS_MS`, `JWT_REFRESH_MS` (optional)
- `PAIRING_SECRET`
- `APP_VERSION` (optional)
- `HTTP_PORT`, `GRPC_PORT`, `GRPC_ENABLED`
- `FIRMWARE_PUBLIC_KEY_PEM` (RSA public key for firmware signature verification)

## Android Signing (CI)
Signing is handled in CI with secrets. The keystore is not stored in the repo.

Required GitHub Secrets:
- `ANDROID_KEYSTORE_BASE64` (base64 of `.jks`)
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Local generation example:
```bash
keytool -genkeypair -v -keystore neogenesis-release.jks \
  -storepass "<password>" -keypass "<password>" -alias neogenesis \
  -keyalg RSA -keysize 4096 -validity 3650 \
  -dname "CN=NeoGenesis Platform, OU=Engineering, O=NeoGenesis S.L., L=Barcelona, S=Barcelona, C=ES"

base64 -w 0 neogenesis-release.jks
```

## Store Deployment Secrets
Google Play:
- `PLAY_SERVICE_ACCOUNT_JSON`

Huawei AppGallery:
- `HUAWEI_CLIENT_ID`, `HUAWEI_CLIENT_KEY`, `HUAWEI_APP_ID`

Amazon Appstore:
- `AMAZON_APPSTORE_CLIENT_ID`, `AMAZON_APPSTORE_CLIENT_SECRET`, `AMAZON_APPSTORE_APP_ID`

Samsung Galaxy Store:
- `SAMSUNG_ACCESS_TOKEN`, `SAMSUNG_SERVICE_ACCOUNT_ID`, `SAMSUNG_CONTENT_ID`, `SAMSUNG_GMS`

## Dev Setup (local)
1. Copy `.env.example` to `.env` and set secrets.
2. Start backend + database:
   - `docker-compose up --build`
3. Desktop:
   - `./gradlew :desktopApp:run`
4. Android:
   - Open `androidApp` in Android Studio or run `./gradlew :androidApp:assembleDebug`

## Security
- JWT with access + refresh tokens
- TLS expected at deployment
- Client defaults are TLS-first; cleartext is only allowed for localhost-style dev endpoints.
- RBAC: Admin, Operator, Researcher, Auditor
- Firmware signature verification during pairing

## Compliance
- Immutable audit logging with hash chaining
- Full telemetry + digital twin metrics per print job
- Evidence export includes hashes for integrity

## Versioning
Semantic version `1.0.0`.

## Device Simulator
Run:
```
python tools/device_simulator.py
```
Or:
```
./gradlew runSimulator
```
Env:
- SIM_JOB_ID, SIM_DEVICE_ID, SIM_RATE_MS
- SIM_GRPC_HOST, SIM_GRPC_PORT

## Client Telemetry
- Desktop app consumes the gRPC telemetry stream.
- Android app uses the HTTP telemetry export endpoint for lightweight viewing.

## Evidence Package Export
Export a reproducible evidence bundle (zip) with manifest + hashes:
```
curl -H "Authorization: Bearer <ACCESS_TOKEN>" \
  http://localhost:8080/api/v1/evidence/<JOB_ID>/package \
  -o evidence-<JOB_ID>.zip
```
Bundle contents include:
- `metadata.json` (version, env snapshot without secrets)
- `audit.json` (hash-chained events)
- `telemetry_samples.json` + `telemetry_summary.json`
- `manifest.json` (SHA-256 hashes per file)

## Tests
- `./gradlew test`
- `./gradlew :backend:test`
- `./gradlew :domain:test`

## Docs
- SECURITY.md
- docs/ARCHITECTURE.md
- docs/RELEASING.md
- docs/SECURITY.md
- docs/REGULATORY_TRACEABILITY.md
- docs/RUNBOOK.md

