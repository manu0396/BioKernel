# MVP Data Model: RegenOps Control App

This document describes the user-facing concepts and state constraints from the
specification. It does not prescribe database schema changes. Existing shared
models and service contracts are authoritative; implementation work must map
these concepts to them and record any mismatch before changing persistence.

## Entities

### Operator Session

Represents the current authenticated operator or explicitly identified demo
session.

- Identity: authenticated subject when signed in; explicit demo identity/state
  for demonstration.
- Attributes: session mode, authorization state, effective role/capabilities,
  token/session validity.
- Constraints: demo and authenticated states are distinguishable; expired,
  denied, and cancelled authorization must not appear authenticated.
- Sensitive values: credentials and raw tokens are never displayed, logged, or
  placed in demonstration artifacts.

### Protocol

Represents a selectable run procedure.

- Identity: protocol identifier.
- Attributes: display name, summary when available, latest/available versions.
- Constraints: selection is limited to a version available to the operator at
  start time; empty and unavailable lists are distinct states.

### Protocol Version

Represents a versioned definition of a protocol.

- Identity: protocol identifier plus version identifier/value.
- Attributes: publication/availability state and human-readable version
  information.
- Constraints: the app identifies the selected version before run start; stale
  or unpublished versions are not silently treated as runnable.

### Device and Gateway

Represents the physical device and gateway used for a connected run.

- Identity: device identifier and gateway identifier.
- Attributes: supported model/configuration, firmware/software version, policy
  tier, connection/security state.
- Constraints: a connected run requires authorization, applicable capabilities,
  an available connection, and a configuration in the pilot's validated-device
  record. One representative device/gateway is the MVP acceptance minimum.

### Run

Represents one simulated or connected execution of a protocol version.

- Identity: stable run identifier; preserve any mapping to job identifier used
  by telemetry, gateway, storage, and exports.
- Attributes: protocol/version, simulated-or-connected mode, device/gateway
  when connected, status, start/finish times when available, operator/session
  association, available record/export state.
- Lifecycle: selected -> start requested -> accepted/active -> paused or
  completed/aborted/failed as confirmed by the service. Exact canonical status
  strings and transition rules must be reconciled against current service
  contracts in Phase 0.
- Constraints: unaccepted requests are not displayed as started; a run cannot
  be mislabeled as physical when simulated; pause/stop views reflect confirmed
  service/device state.

### Telemetry Observation

Represents an observation associated with a run and source device.

- Identity: run/job identifier, device identifier, and source timestamp; use
  protocol-defined sequence or deduplication data if the current contract
  provides it.
- Attributes: measured values, source timestamp, latest-received time, stream
  connection/freshness state.
- Constraints: stale observations remain visibly old; reconnecting or receiving
  delayed frames does not imply they are current. Set the numeric stale
  threshold after confirming device cadence in Phase 0.

### Evidence Export

Represents a report or evidence package requested for one run.

- Identity: selected run identifier and export kind.
- Attributes: export status, format/content type, filename, integrity metadata
  when supplied by the existing service.
- Constraints: report and evidence package remain associated with the selected
  run; role/capability denial, empty data, or failed integrity checks must not
  be shown as a successful export.

## Relationships

- An operator session performs actions on protocols, runs, and exports subject
  to its roles and device capabilities.
- A protocol has one or more versions; a run references exactly the selected
  protocol/version pair.
- A connected run references a device and gateway; a simulated run is explicitly
  marked and does not claim physical-device provenance.
- A run has zero or more telemetry observations and zero or more available
  exports.
- Evidence records and export integrity metadata refer to the same run identity
  used by the run list and telemetry stream.
