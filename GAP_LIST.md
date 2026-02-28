# GAP_LIST.md

## P0 (Must Fix for Production)
- Auth/session: no token refresh/expiry handling; tokens only stored and cleared on logout. Estimated effort: 2-3 days.
- Environment config: no dev/stage/prod profiles; cleartext HTTP allowed by default. Estimated effort: 1-2 days.
- Device registry/pairing UI missing; no device list, status, or pairing flows in client. Estimated effort: 3-5 days.
- Client-side audit trail missing (no event hashing or chain linkage). Estimated effort: 3-4 days.

## P1 (High Value / Reliability)
- Telemetry resilience: no reconnection/backoff or stale/connected UI state. Estimated effort: 2-3 days.
- Android telemetry: only HTTP export snapshot; no streaming. Estimated effort: 3-5 days depending on transport choice.
- Commands/control: no explicit acks/timeouts/idempotency in client. Estimated effort: 2-3 days.
- Evidence export UI workflow missing (download bundle, show manifest/hash verification hints). Estimated effort: 2-3 days.
- Offline tolerance: no caching of last device list or telemetry snapshot. Estimated effort: 2-4 days.

## P2 (Quality / Observability)
- Structured client logging with correlation IDs. Estimated effort: 1-2 days.
- Crash reporting hooks. Estimated effort: 1 day.
- Ktlint formatting violations across modules (currently ignored). Estimated effort: 2-4 days depending on scope.

## Next 2 Weeks Plan (Recommended)
1) Implement token refresh + expiry awareness, and add environment config profiles (`dev/stage/prod`) with TLS-by-default.
2) Build device registry + pairing UI with clear status and retry.
3) Add client audit-event model + hash chain append, persisted locally and included in evidence export UI.
4) Telemetry resiliency: reconnection/backoff, stale indicators, bounded buffer, and Android streaming strategy (gRPC on JVM; fallback WebSockets/HTTP2 streaming on Android if needed).
5) Add evidence export UI and a verification guide surface in-app.
6) Apply ktlint fixes or generate baselines and ratchet.