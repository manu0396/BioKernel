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

## Device Policy
- Config file: `services/core-server/src/main/resources/device-policy.yaml`
- Apply changes by redeploying core-server (policy is loaded on startup).
- If device headers are missing, server defaults to `DeviceClass.UNKNOWN` and `Tier2` safe mode (control/edit/admin denied).

## Evidence Package
Export:
- `GET /api/v1/evidence/{jobId}/package` (requires JWT)

Verify:
- Unzip the archive.
- Confirm `manifest.json` hashes match file contents (SHA-256).
- Validate `audit.json` chain using `EvidenceChainValidator` from `domain`.
## mTLS Rotation (gRPC)
- gRPC service-to-service traffic uses mTLS. HTTP ingress terminates TLS at the ingress controller.
- Certificates are issued and rotated by cert-manager (see `ops/k8s/cert-manager/`).
- Short-lived certs: `duration: 24h`, `renewBefore: 6h`.
- gRPC server reloads certificates on file changes (default check every 5 minutes; `GRPC_TLS_RELOAD_INTERVAL_SEC`).

Runbook checks:
1. `kubectl get certificates -A` and verify `READY=True`.
2. Inspect secret `neogenesis-grpc-mtls` and confirm `tls.crt` is renewed.
3. Verify gRPC connectivity from gateway with a client cert.

Rollback (mTLS):
1. Scale down the gateway and core-server deployments.
2. Revert cert-manager resources to the last known-good issuer/cert manifests.
3. Restore the previous secret version (or re-issue with known-good CA).
4. Scale services back up and verify handshake.
