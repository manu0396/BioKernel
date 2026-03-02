# Pilot Install Checklist (BioKernel)

1. Confirm `VERSION` is `1.0.0` and `CHANGELOG.md` includes Phase 4 notes.
2. Configure core-server env:
   - `JWT_SECRET`, `PAIRING_SECRET`, database settings, TLS settings.
3. Validate `device-policy.yaml` matches deployment policy.
4. Bring up backend + Postgres:
   - `docker-compose up --build`
5. Run health probes:
   - `GET /health`, `GET /health/ready`
6. Verify metrics:
   - `GET /metrics`
7. Validate device-tier enforcement:
   - Tier2/Tier3 cannot control or edit.
8. Verify evidence export:
   - `GET /api/v1/evidence/{jobId}/package`
9. Confirm gateway connects with mTLS and reports health.
10. Run automated tests:
   - `./gradlew.bat test`

