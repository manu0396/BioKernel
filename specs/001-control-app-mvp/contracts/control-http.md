# Control App HTTP Compatibility Surface

**Purpose**: Identify existing HTTP integration points for the MVP and the
compatibility checks to perform. This is not a new API proposal or an OpenAPI
replacement.

## Control Run and Protocol Routes

Current server implementation: `services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/RegenOpsHttpModule.kt`.

| Method | Path | Purpose | Compatibility / MVP check |
|---|---|---|---|
| GET | `/api/v1/regenops/protocols` | List protocol summaries | Verify version availability/details exposed to the client and empty/error distinction. |
| GET | `/api/v1/regenops/runs` | List run records | Verify identity, status, mode, and device mapping needed by history. |
| POST | `/api/v1/regenops/runs/start` | Request a run | Confirm whether connected requests dispatch to gateway/device; response acceptance is not proof of physical execution. |
| POST | `/api/v1/regenops/runs/{id}/pause` | Request pause | Verify authorization, allowed state transitions, and confirmed result. |
| POST | `/api/v1/regenops/runs/{id}/abort` | Request stop/abort | Verify authorization, allowed state transitions, and confirmed terminal result. |
| POST | `/api/v1/regenops/protocols/{id}/publish` | Publish selected version | Verify whether publication is required for operator run selection; preserve role/capability enforcement. |

Routes apply `READ_ONLY_DASHBOARD`, `PRINT_CONTROL`, or `PROTOCOL_EDIT`
capability checks as implemented. Validate current role-to-capability policy;
do not weaken it to simplify the MVP.

## Authentication

The Control documentation describes OIDC device authorization. The core also
has `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, and
`/api/v1/auth/logout`. Phase 0 must prove which identity-provider flow the
Control client uses and how resulting identity/role/capability claims authorize
the protected Control routes. Do not assume the local username/password routes
are the app's sign-in contract.

## Run Exports

Current implementation:
`services/core-server/src/main/kotlin/com/neogenesis/platform/core/modules/ExportsAliasModule.kt`.

| Method | Path | Output | Authorization check |
|---|---|---|---|
| GET | `/api/v1/telemetry/{runId}/export` | CSV report | Role and `QC_REVIEW` capability |
| GET | `/api/v1/evidence/{runId}/package` | ZIP evidence package | Admin/auditor role and `QC_REVIEW` capability |

Verify content type, attachment filename, run association, empty-data behavior,
integrity metadata, denied access, and errors before relying on exports in the
pilot.

## Contract Gate

Before implementation, record request/response shapes from the current DTOs and
client, plus HTTP failure semantics. Tests must cover client compatibility and
authorization. Any contract change must be additive where possible and include
an explicit compatibility and migration/rollback rationale.
