# Security

- JWT access + refresh tokens.
- RBAC roles: ADMIN, OPERATOR, RESEARCHER, AUDITOR.
- Device pairing uses HMAC challenge/response with `PAIRING_SECRET`.
- Passwords hashed with bcrypt.
- Tokens can be revoked via logout.

## Threat Model (Summary)
- Credential theft: mitigated by bcrypt + refresh revocation.
- Device spoofing: pairing secret and signed firmware checks.
- Audit tampering: hash-chained evidence logs.

## mTLS Boundary
- Internal gRPC service-to-service traffic uses mTLS with short-lived certs.
- Public HTTP ingress terminates TLS at the ingress controller.
- Cert rotation is automated via cert-manager (see `ops/k8s/cert-manager/`).
