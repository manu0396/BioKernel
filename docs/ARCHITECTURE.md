# Architecture

NeoGenesis Platform is a Kotlin Multiplatform system composed of:
- `domain`: domain, telemetry, evidence, digital twin, crypto.
- `data`: DTOs and mapping layer.
- `shared-network`: KMP networking, retry, timeout, error mapping.
- `backend`: Ktor + gRPC, persistence, RBAC, audit trail.
- `firmware-protocol`: protobuf contracts for firmware + platform streams.
- `androidApp` and `desktopApp`: Compose Multiplatform UI.

## Data Flow
1. Device streams telemetry via gRPC `FirmwareBridge` to backend.
2. Backend persists telemetry and publishes to `TelemetryBus`.
3. UI subscribes via `TelemetryStreamService` and visualizes downsampled data.
4. Print job events are published and stored.
5. Evidence events are hash-chained and exported.

## Modules
- Auth/User/Device/Telemetry/PrintJob/DigitalTwin/Evidence/HospitalIntegration/AdminOps.

## Determinism
Digital Twin engine is pure Kotlin and deterministic for given parameters.

## gRPC Compatibility
- gRPC contracts are sourced from the `neogenesis-contracts` artifact.
- Bump the contracts version for any breaking wire changes and keep server/client in lockstep.
- Prefer additive, backward-compatible changes for pilot environments.
