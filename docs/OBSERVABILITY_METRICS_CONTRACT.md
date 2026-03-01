# Observability Metrics Contract

This document defines the metrics and labels used for RUO/enterprise dashboards. No PHI/PII is emitted.

## Required Labels (Business Metrics)
- `tenant_id`: anonymized tenant identifier (no PHI).
- `site_id`: anonymized site identifier (no PHI).
- `cohort_id`: anonymized cohort identifier (no PHI).
- `protocol_id`: protocol identifier (non-PHI).
- `protocol_version`: protocol version integer.

Labels are populated from HTTP headers (`X-Tenant-Id`, `X-Site-Id`, `X-Cohort-Id`, `X-Protocol-Id`,
`X-Protocol-Version`) and gRPC metadata (`x-tenant-id`, `x-site-id`, `x-cohort-id`, `x-protocol-id`,
`x-protocol-version`). Missing labels are set to `unknown`.

## Metrics
### Run Lifecycle
- `neogenesis_runs_started_total`
- `neogenesis_runs_completed_total`
- `neogenesis_runs_failed_total`
- `neogenesis_runs_paused_total`
- `neogenesis_runs_resumed_total`
- `neogenesis_runs_retry_total`
- `neogenesis_run_duration_ms` (timer) with label `outcome=success|failure`

### Evidence Export
- `neogenesis_evidence_exports_total` with label `outcome=success|failure`

### Gateway Telemetry/Events
- `neogenesis_gateway_events_total` with labels `event_type`, `source`
- `neogenesis_telemetry_frames_total` with label `metric_key`

### HTTP + gRPC SLO Signals
- `neogenesis_http_requests_total` with labels `path`, `method`, `status`
- `neogenesis_http_request_duration_ms` (timer) with labels `path`, `method`, `status`
- `neogenesis_grpc_requests_total` with labels `grpc_service`, `grpc_method`, `status`
- `neogenesis_grpc_request_duration_ms` (timer) with labels `grpc_service`, `grpc_method`, `status`

## Privacy Guarantees
- No PHI/PII is emitted in metric labels or values.
- All identifiers are anonymized or internal IDs.
- Payloads and contents are never included in labels.
