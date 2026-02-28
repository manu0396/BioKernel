# Security Policy

## Supported Versions
- `1.0.x`: Supported

## Reporting a Vulnerability
- Contact: `security@neogenesis.example`
- Preferred format: reproducible steps, affected module(s), impact, and logs without secrets.
- Do not publish exploit details before coordinated disclosure.

## Response Targets
- Initial acknowledgment: within 3 business days.
- Triage/update: within 7 business days.
- Fix timeline: severity-based and coordinated with release owners.

## Security Baseline
- TLS is required by default for client-server communication.
- Android token storage uses encrypted storage primitives.
- Desktop token storage is encrypted at rest with environment-key override support:
  - `NEOGENESIS_DESKTOP_TOKEN_KEY` (recommended for managed deployments).
  - If missing, a local encryption key is generated under `~/.neogenesis/tokens.key`.
- JWT access + refresh token flow with logout revocation.
- No repository-committed signing keys or production secrets.

## Secret Handling
- Configure runtime secrets via environment variables only.
- Never commit `.env`, keystores, token dumps, or private keys.
- CI release signing is env/secret driven (`ANDROID_KEYSTORE_*`, `NEOGENESIS_KEY*`).

## Dependency Risk Management
- Pull requests run dependency review in CI.
- Before release, validate dependency changes and advisories from CI and lockfile/versions updates.
