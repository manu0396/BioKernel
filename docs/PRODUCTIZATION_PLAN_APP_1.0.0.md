# Productization Plan: App Monetization v1.0.0

## Scope
- Billing UX (Subscribe / Manage) via server-provided Stripe URLs.
- Entitlement fetch + cache + reactive state.
- Premium feature gating with upgrade paywall UX.
- Tests + release docs.

## Constraints
- Server-driven billing only; no native IAP.
- Never log tokens or billing URLs with sensitive query params.
- Keep existing architecture and apply minimal, reviewable commits.

## Discovery Summary
- Networking layer: `shared-network` Ktor APIs with `safeApiCall`.
- Auth token storage: secure on Android (`EncryptedSharedPreferences`) and encrypted on Desktop (`DesktopTokenStorage`).
- Client screens: no dedicated settings route yet; both clients use a single `RootScreen` with panels.

## Planned Commit Sequence
1. `plan(monetization): add execution plans`
- Update `PLANS.md`.
- Add `docs/PRODUCTIZATION_PLAN_APP_1.0.0.md`.

2. `feat(network): add billing api + models`
- Add billing DTOs in `data`.
- Add `BillingApi` + Ktor implementation in `shared-network`.
- Add feature flags and entitlement status models.

3. `feat(network): add entitlements repository with ttl + grace`
- Add `EntitlementsRepository` with:
  - Startup/browser-return refresh API.
  - TTL cache (default 10 min).
  - Offline grace window (24h) only for last known active status.
  - `StateFlow` exposure for UI.

4. `feat(android): add upgrade screen and paywall gating`
- Integrate Upgrade view and Account entry point in Android `RootScreen`.
- Wire Subscribe/Manage actions to external browser.
- Gate premium entry points and route missing entitlement to paywall.

5. `feat(desktop): add upgrade screen and paywall gating`
- Mirror Android behavior in Desktop `RootScreen`.
- Open Stripe URLs externally using desktop browser integration.

6. `test(shared-network): add entitlements repository tests`
- Add fake `BillingApi`.
- Validate fresh cache, TTL expiry refresh, offline grace active/inactive behavior, failure state handling.

7. `docs(release): billing client + release validation updates`
- Add `docs/BILLING_CLIENT.md`.
- Update `docs/RELEASING.md` with paywall and entitlement refresh checks.

8. `chore(verify): run release verification commands`
- Run required Gradle commands.
- Run desktop app and perform manual Upgrade/Manage/paywall checks.
- Record evidence and known environment limitations.

## Verification Checklist
- `./gradlew clean build`
- `./gradlew.bat test`
- `./gradlew.bat :backend:test`
- `./gradlew.bat :domain:test`
- `./gradlew.bat :shared-network:test`
- `./gradlew.bat :desktopApp:run`
- `./gradlew.bat :androidApp:assembleDebug`
- Manual:
  - Upgrade screen accessible from main authenticated area.
  - Subscribe opens external browser from checkout URL.
  - Manage opens external browser from portal URL.
  - Premium features show paywall when entitlement missing.
  - Entitlements refresh after returning from browser flow.

## Rollback
- Revert billing-related files from commits 2-7.
- Keep only planning/doc files if needed for future iteration.
- Re-run `./gradlew clean build` to confirm stable baseline.
