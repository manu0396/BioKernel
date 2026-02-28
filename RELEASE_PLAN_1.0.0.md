# Release Plan 1.0.0

## Release Root Decision
- **Selected release root:** `BioKernel/`
- **Evidence:**
  - `settings.gradle.kts` includes production modules: `:androidApp`, `:desktopApp`, `:domain`, `:data`, `:shared-network`, `:backend`, `:firmware-protocol`.
  - KMP targets are configured in shared modules (`domain`, `data`, `shared-network`) with `androidTarget` and `jvm("desktop")`.
  - Runnable entry points exist for both release targets:
    - Android app module: `androidApp`
    - Desktop distributables: `desktopApp` with Compose Desktop packaging tasks.
  - CI and release automation are wired in `.github/workflows/ci.yml` and `.github/workflows/release.yml`.
- **Other candidates (not release root):**
  - `CMP-Bookpedia/`: standalone KMP sample app, separate repo and no CI workflows in that folder.
  - `compose-multiplatform-template/`: template project.

## Environment Findings
- `java -version` initially failed (`java` missing in Linux environment).
- Installed portable Linux JDK 17 at `/tmp/jdk17/jdk-17.0.18+8`.
- `./gradlew --version` verified with Gradle `8.11.1` on JDK 17.
- Local workspace constraint: running Gradle directly from `/mnt/c/...` can fail (`buildSrc:processResources` chmod issue on Windows mount); validation is run from Linux FS mirror (`/tmp/BioKernel-release`).

## Baseline Findings / Risks
- **Build/Reproducibility**
  - Gradle wrapper locked to `8.11.1` (good).
  - Kotlin/Compose/AGP versions are centrally managed in `gradle/libs.versions.toml` (good), but Kotlin MPP scripts still use deprecated `kotlinOptions` APIs.
  - CI does not currently enforce full quality gate suite (`ktlint`, `detekt`, lint, build) as mandatory check set.
- **Versioning/Metadata**
  - Root version is already `1.0.0`; Android versionName is `1.0.0`; Desktop packageVersion is `1.0.0`.
  - Android `versionCode = 1` exists, but release docs/tag workflow need explicit, reproducible instructions.
- **Code Quality Gates**
  - `detekt` and `ktlint` are applied but configured with `ignoreFailures = true`.
  - `detekt.yml` disables nearly all rule groups.
- **Testing**
  - `commonTest` exists in `domain` and `shared-network`.
  - Networking tests exist (`ApiCallTest`) but no HttpClient smoke test validating timeout/TLS guard behavior.
- **Stability/UX**
  - Base URL defaults in Android/Desktop UIs use cleartext HTTP and explicitly set `allowCleartext = true`.
  - Error handling exists, but defaults are not production-safe.
- **Security/Privacy**
  - Android token storage uses encrypted prefs (good).
  - Desktop token storage currently writes plaintext properties.
  - Root `SECURITY.md` required by release checklist is missing (only `docs/SECURITY.md` exists).
- **Observability**
  - Backend has structured logging (logback/logstash), but shared client logging abstraction and redaction guidance are not explicit.
- **CI/CD**
  - Existing CI job runs selected tests/compile only; missing dedicated lint/detekt task, release-signing preflight checks, and vulnerability scan step.
- **Packaging/Distribution**
  - Android release signing via env vars is present.
  - Desktop packaging tasks exist and are used in release workflow.

## Work Items by Requirement Area

### 1) Build/Reproducibility
- Keep JDK 17 as canonical toolchain in docs and CI.
- Ensure consistent JVM target/toolchain settings and remove deprecated KMP DSL usage where feasible.
- Document Linux FS caveat for WSL local builds in release docs.

### 2) Versioning/Metadata
- Confirm all shipped targets remain at `1.0.0`.
- Extend `CHANGELOG.md` with release-quality notes and verification scope.
- Add explicit `v1.0.0` tagging workflow in `docs/RELEASING.md`.

### 3) Code Quality Gates
- Set `detekt` and `ktlint` to fail builds in CI.
- Tighten detekt config to enforce critical categories (bugs, exceptions, coroutines); keep pragmatic scope.
- Add warnings-as-errors toggle for CI builds (`-PwarningsAsErrors=true`) and wire compiler options accordingly.

### 4) Testing
- Add shared networking smoke tests for `HttpClientFactory` behavior (cleartext guard + timeout path).
- Keep existing common tests and include them in CI quality command set.

### 5) Stability/UX
- Change client defaults to TLS URLs and default `allowCleartext=false` for runtime config.
- Preserve explicit dev override path for local HTTP testing.
- Ensure desktop telemetry stream lifecycle properly closes resources (already mostly present; verify).

### 6) Security/Privacy
- Harden desktop token storage with best-effort encryption/obfuscation + strict file permissions and documented limitations.
- Add root `SECURITY.md` with disclosure policy and secure configuration baseline.
- Add dependency vulnerability scanning step to CI (OWASP dependency-check).

### 7) Observability
- Add minimal shared logger abstraction with redaction helper for client-side logs.
- Use logger in network error path to avoid raw sensitive payload logging.

### 8) CI/CD
- Update CI workflow to run:
  - format/lint checks (`ktlintCheck`, Android lint)
  - static analysis (`detekt`)
  - tests/build for Android + Desktop + shared modules
  - dependency vulnerability scan
- Keep Gradle caching and JDK 17 setup.

### 9) Packaging/Distribution
- Verify and document exact Android/Desktop artifact commands.
- Ensure release signing gate remains enforced for release tasks.

## Proposed Small-Commit Sequence
1. **docs(release):** add `RELEASE_PLAN_1.0.0.md` and baseline findings.
2. **build(quality):** enforce `detekt`/`ktlint` in CI mode; warnings-as-errors property support.
3. **security(client):** remove cleartext-by-default client configs; harden desktop token storage.
4. **test(network):** add shared-network smoke tests for TLS guard/timeout semantics.
5. **ci(gates):** expand CI workflow for lint/detekt/build/test/security scan.
6. **docs(release):** add `SECURITY.md`, `docs/RELEASING.md`, update `CHANGELOG.md`.
7. **final verification:** run verified Gradle command set and capture release checklist.

## Verification Commands (Target)
- `./gradlew clean build`
- `./gradlew ktlintCheck detekt lint`
- `./gradlew :domain:allTests :data:allTests :shared-network:allTests :backend:test :androidApp:testDebugUnitTest :desktopApp:test`
- `./gradlew :androidApp:bundleRelease :androidApp:assembleRelease`
- `./gradlew :desktopApp:packageReleaseDistributionForCurrentOS`

