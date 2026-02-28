# ADR 0001: KMP + Ktor + gRPC

Decision: Kotlin Multiplatform domain/data/shared-network core with Ktor backend and gRPC streaming.
Rationale: single language, deterministic core, high-rate telemetry with gRPC.
Consequences: domain models reused across backend and UI, data/network layers shared by clients.
