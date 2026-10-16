# MVP Pilot Validation Quickstart

This guide is an acceptance sequence for the controlled pilot, not a substitute
for the detailed release runbook. Use synthetic/demo data; do not use patient or
biometric data in development or pilot demonstrations.

## Prerequisites

- JDK 21/Gradle wrapper and Android SDK configured for app checks.
- Pilot OIDC issuer/client/audience confirmed; test accounts/roles provisioned.
- A supported physical device and gateway available for the connected run.
- TLS/network configuration, PostgreSQL, and service environment values prepared
  outside source control.
- The exact supported Android flavor/build variant and current Gradle task names
  verified from Gradle before using commands below. The project defines both
  client and store flavor dimensions.

## 1. Start Core Services

1. Copy `.env.example` to the repository-root `.env` and set non-production
   pilot secrets and connection settings; do not commit `.env`.
2. Confirm the root `.env` path and repository-root build context configured in
   `ops/docker-compose.yml`, then verify Android, gateway, and backend HTTP/gRPC
   hosts and ports match the intended environment. Do not run the pilot against
   unverified defaults.
3. Validate Compose's resolved settings:

   ```bash
   docker compose --env-file .env -f ops/docker-compose.yml config
   ```

4. Start Postgres and core service:

   ```bash
   docker compose --env-file .env -f ops/docker-compose.yml up --build postgres core-server
   ```

5. Verify the documented health endpoints before launching the app.

## 2. Build and Launch the App

Run from repository root, after verifying variant/task names:

```bash
./gradlew :apps:control-kmp:desktopApp:run
./gradlew :apps:control-kmp:androidApp:tasks --all
```

Select the pilot's demo or enterprise client/store flavor as appropriate; do not
assume a generic `assembleDebug` variant exists with the configured flavor
dimensions. Record the exact Android launch/install command in the pilot
acceptance record.

For the mock-backed demo/test build only, assemble the labeled Android demo
variant and run deterministic mock tests:

```bash
./gradlew :apps:control-kmp:androidApp:assembleDemoGoogleDebug -PDEMO_MODE=true --rerun-tasks
./gradlew :apps:control-kmp:shared:jvmTest --tests '*OidcRepositoryTest' \
  :agents:device-gateway:test :services:core-server:test
```

The OIDC MockEngine and mock gateway frames are test fixtures; they do not
authenticate a user, issue a physical-device command, or satisfy pilot
acceptance.

## 3. Validate Operator Access and Protocol Selection

1. Start unauthenticated; verify OIDC authorization and visible cancellation or
   failure behavior.
2. Enter demo mode separately; confirm the app marks it as demo.
3. Verify protocol search, refresh, version selection, and empty/error states.
4. Verify unauthorized actions remain unavailable and failures are not reported
   as successes.

## 4. Validate Simulated Run

1. Select a protocol/version and explicitly choose simulation.
2. Start the run and verify it is labeled simulated.
3. Verify active status, telemetry freshness state, pause, and stop.
4. Interrupt/recover the stream and verify stale observations are not shown as
   newly current.

## 5. Validate Connected Physical Run

1. Record the physical device model, firmware, gateway version, device tier,
   TLS/mTLS mode, and environment.
2. On Android and desktop, start a run with an authorized operator and selected
   protocol/version using the same validated device configuration.
3. On each app target, verify gateway health and device response, then confirm
   telemetry reaches the app with the same run identity.
4. On each app target, pause/stop and verify confirmed device/service state.
5. Do not count a simulator or a REST success response alone as physical-device
   acceptance.

## 6. Validate History, Exports, and Release Gates

1. Select a completed run; verify run mode, status, and identity.
2. Export CSV and evidence ZIP; verify association with the selected run,
   content, and integrity metadata.
3. Verify denied/empty/failing export behavior.
4. Complete the 5-person usability test; at least 4 operators must finish the
   sign-in-or-demo, run, monitor, and stop journey without assistance.
5. Complete security/privacy review and confirm pilot status and limitations are
   documented.
6. Confirm root, Android, desktop, and release version metadata agree for the
   mock-backed v1.0.0 demo/test artifact. Do not label it pilot-approved until
   all real acceptance gates pass.

## Verification Commands

Repository CI baseline:

```bash
./gradlew test --no-daemon
```

Also run the verified current-path tasks for shared app tests, core-server tests,
Android unit/UI tests, desktop tests, and the release build. Record exact
commands and outcomes; distinguish emulator/desktop tests from physical-device
evidence.
