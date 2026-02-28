# Contributing

## Development Setup
1. Install JDK 17.
2. Configure Android SDK (for Android module checks).
3. Copy `.env.example` to `.env` for local backend runtime values (do not commit `.env`).

## Required Checks Before PR
```bash
./gradlew --no-daemon -PstrictQuality=true -PwarningsAsErrors=true \
  ktlintCheck detekt :androidApp:lintDebug \
  :domain:allTests :data:allTests :shared-network:allTests \
  :backend:test :androidApp:testDebugUnitTest :desktopApp:test
```

## Guidelines
- Keep changes atomic and module-scoped.
- Add/update tests for shared logic and network behavior.
- Do not commit secrets, keystores, or credentials.
- Update `CHANGELOG.md` for user-visible changes.
