# Device, Run, and Telemetry Stream Compatibility Surface

**Purpose**: Record existing protobuf surfaces that participate in a connected
run. Source contracts remain in `shared/proto/src/main/proto/`.

## Existing Contracts

| Contract | Existing source | MVP purpose |
|---|---|---|
| `FirmwareBridge` | `firmware.proto` | Gateway/device telemetry and health streams; server-to-gateway command stream and command acknowledgement. |
| `RunService` | `regenops.proto` | Start/pause/abort/get run, stream run events, stream run telemetry. |
| `TelemetryStreamService` | `platform.proto` | Bidirectional telemetry stream between client/service according to existing contract. |
| Device policy | `device_policy.proto` | Device policy/capability context; verify current enforcement on connected actions. |

## Compatibility Requirements

- Preserve protobuf package, service, message, and field-number compatibility.
- Verify service directionality and the actual client/server implementation; a
  service declared in a proto is not evidence that the MVP uses it end to end.
- Trace identifiers across app run, backend run/job, gateway command, telemetry,
  and evidence export. Record any mapping or conversion.
- Verify mTLS/TLS and device-policy enforcement for the selected pilot
  environment; do not expose keys or certificates in the app or docs.
- Connected-device acceptance requires a captured test record for one physical
  device/gateway configuration and a corresponding run/telemetry result.

## Required End-to-End Evidence

1. Authorized app request for the selected protocol/version and physical device.
2. Service acceptance with a stable run identifier.
3. Gateway connection/health visible to the service.
4. Device command/acknowledgement where the protocol requires control.
5. Device telemetry associated with the same run/job and visible in the app.
6. Confirmed pause/stop result and correct terminal state.

The Phase 0 trace must identify any missing link before implementation tasks
are treated as ready.
