# Grafana Business Dashboards

## Location
- Dashboards: `ops/grafana/dashboards/`
- Recording rules: `ops/prometheus/rules/`

## Import
1. Open Grafana and go to Dashboards -> Import.
2. Upload `ops/grafana/dashboards/regenops-business.json`.
3. Select the Prometheus data source.

## Expected Labels
Dashboards rely on the metrics contract in `docs/OBSERVABILITY_METRICS_CONTRACT.md`.
Ensure the following labels are populated on incoming requests:
- `tenant_id`, `site_id`, `cohort_id`, `protocol_id`, `protocol_version`

## No PHI
Dashboards are designed to operate without PHI/PII. Use anonymized IDs only.
