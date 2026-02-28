# Billing Client (Server-Driven Stripe)

## Overview
The client does not implement native in-app purchases. Billing is server-driven via hosted Stripe URLs.

Endpoints used by the client:
- `POST /billing/checkout-session` -> `{ "url": "..." }`
- `POST /billing/portal-session` -> `{ "url": "..." }`
- `GET /billing/status` -> `{ "plan": "...", "status": "...", "periodEnd": "...", "entitlements": ["..."] }`

## Runtime Flow
1. User opens `Account / Upgrade`.
2. Client loads entitlement state from `EntitlementsRepository`.
3. User taps:
- `Subscribe`: client requests checkout URL, then opens external browser.
- `Manage subscription`: client requests portal URL, then opens external browser.
4. User returns to app and triggers refresh (automatic on app start, explicit refresh on Upgrade screen).

## Entitlements Repository Behavior
- Reactive state exposed with `StateFlow`.
- Cache TTL defaults to 10 minutes.
- Offline grace defaults to 24 hours, and only applies when the last known subscription status was active/trialing.
- Premium UI gating reads repository state for UX-only gating; backend authorization remains source of truth.

## Feature Flags
Client-side premium gates are based on:
- `AUDIT_EXPORT`
- `ADVANCED_TELEMETRY_EXPORT`
- `MULTI_DEVICE`

## Security Notes
- Never log raw billing URLs (especially with query parameters).
- Never log auth tokens.
- Network client is TLS-first; cleartext is only allowed for localhost-style development endpoints.
- Token storage is platform-secure where available:
  - Android: `EncryptedSharedPreferences`
  - Desktop: encrypted local token payload using AES-GCM

## Local Testing
Suggested checks:
1. Login and open `Account / Upgrade`.
2. Validate status/plan rendering.
3. Click `Subscribe` and `Manage subscription` and verify browser opens.
4. Return to app and click `Refresh status`.
5. Confirm premium-gated actions show paywall when entitlement is missing.

If backend billing is disabled (for example `BILLING_ENABLED=false` on server), expect billing endpoints to fail or return non-active status; the client should show unavailable/locked premium state without crashing.
