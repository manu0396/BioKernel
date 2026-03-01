# Releasing NeoGenesis Platform

This document describes the production release flow for `v1.0.0` and later.

## Prerequisites
- JDK 17 available.
- Android SDK installed.
- Required signing/deploy secrets configured in CI.
- Clean working tree.

## 1) Bump and Verify Version
1. Confirm project version metadata:
   - Root: `build.gradle.kts` (`version = "1.0.0"` or next target).
   - Android: `androidApp/build.gradle.kts` (`versionName`, `versionCode`).
   - Desktop: `desktopApp/build.gradle.kts` (`packageVersion`).
2. Update `CHANGELOG.md` with release date and highlights.

## 2) Run Quality Gates Locally
```bash
./gradlew --no-daemon -PstrictQuality=true -PwarningsAsErrors=true \
  clean ktlintCheck detekt :androidApp:lintDebug \
  :domain:allTests :data:allTests :shared-network:allTests \
  :backend:test :androidApp:testDebugUnitTest :desktopApp:test
```

## 3) Build Release Artifacts
Android release:
```bash
./gradlew --no-daemon :androidApp:bundleRelease :androidApp:assembleRelease
```

Desktop release package (current OS):
```bash
./gradlew --no-daemon :desktopApp:packageReleaseDistributionForCurrentOS
```

## 4) Tag and Push
```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

Tag push triggers `.github/workflows/release.yml`.

## 5) Artifact Locations
- Android AAB: `androidApp/build/outputs/bundle/release/*.aab`
- Android APK: `androidApp/build/outputs/apk/release/*.apk`
- Desktop packages: `desktopApp/build/compose/binaries/main-release/**`
- Backend fat jar: `backend/build/libs/backend-all.jar`

## 6) Post-Release Checks
1. Verify CI release workflow green.
2. Validate uploaded artifacts/signatures.
3. Confirm changelog/tag correctness.
4. Announce release and archive run metadata.

## 7) Billing/Paywall Validation
Run client-focused monetization checks before final sign-off:
1. Launch desktop app: `./gradlew.bat :desktopApp:run`.
2. Login and open `Account / Upgrade`.
3. Confirm status block renders:
   - plan
   - status
   - period end
   - entitlements list
4. Click `Subscribe` and verify external browser opens Stripe checkout.
5. Click `Manage subscription` and verify external browser opens Stripe portal.
6. Return to app and refresh entitlement state.
7. Validate premium-gated actions show paywall when entitlement is missing.
8. Validate premium-gated actions are available when entitlement is present.

## 8) Security/Observability Checks
1. Confirm gRPC mTLS is enabled in the target environment (short-lived certs).
2. Validate `/metrics` is scraped and Grafana dashboards import cleanly.
3. Confirm Android release builds have backups disabled.
