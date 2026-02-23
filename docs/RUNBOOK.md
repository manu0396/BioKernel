# Runbook

## Start stack
- `docker-compose up --build`

## Migrations
- Flyway runs on backend startup.

## Backup
- Use PostgreSQL `pg_dump` daily.

## Rotate Secrets
- Rotate JWT secret and pairing secret quarterly.

## Troubleshooting
- Check `/health` and `/ready`.
- Inspect logs (JSON) for correlation id `cid`.

## Evidence Package
Export:
- `GET /api/v1/evidence/{jobId}/package` (requires JWT)

Verify:
- Unzip the archive.
- Confirm `manifest.json` hashes match file contents (SHA-256).
- Validate `audit.json` chain using `EvidenceChainValidator` from `domain`.
