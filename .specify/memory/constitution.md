<!--
Sync Impact Report
Version change: none -> 1.0.0 (initial constitution)
Modified principles: none; all core principles are newly established
Added sections: Security and Data Protection; Engineering Workflow and Validation
Removed sections: none
Follow-up TODOs: Confirm the original ratification date.
-->
# BioKernel Engineering Constitution

## Core Principles

### I. Preserve Module Boundaries
Implement changes in the Gradle module that owns the behavior and keep changes
atomic and module-scoped. Reuse shared domain, data, network, and protocol
contracts instead of duplicating them across applications and services. This
keeps the monorepo's app, service, agent, and shared-library boundaries explicit.

### II. Verify Behavior with Tests
Add or update tests for changed behavior, especially shared logic, network
behavior, API contracts, and inter-service communication. Run the narrowest
relevant Gradle tests and the repository's CI test command before considering
the change complete. Tests provide evidence that changes work across the
repository's multiplatform and service boundaries.

### III. Protect Secrets and Sensitive Data
Use environment-driven runtime configuration for secrets. Never commit `.env`
files, signing keys, keystores, private keys, token dumps, or production
credentials. Do not expose credentials or sensitive biometric or retinal data
in logs, fixtures, or generated artifacts. These controls reduce the risk of
account compromise and disclosure of sensitive data.

### IV. Maintain Compatibility and Data Integrity
Preserve established API, protocol, and persisted-data behavior unless a change
explicitly requires otherwise. When changing a contract, migration, or audit
behavior, document compatibility impact and provide appropriate tests. This
protects existing clients and the integrity of stored and exported records.

### V. Keep Changes Reviewable and Documented
Prefer the smallest complete change that follows existing project conventions.
Surface errors rather than hiding failures. Update relevant user or operational
documentation when behavior, configuration, or release procedures change.
Reviewable changes and accurate documentation make the system easier to operate
and maintain.

## Security and Data Protection

Client-server communication MUST use TLS by default. Authentication tokens MUST
use the repository's encrypted storage mechanisms. Runtime secrets MUST be
provided through environment variables or the configured CI secret mechanism,
not committed to the repository. CI dependency review and advisory checks MUST
be considered when dependencies change and before release. Follow `SECURITY.md`
for vulnerability reporting and coordinated disclosure.

## Engineering Workflow and Validation

Keep pull requests focused and module-scoped. Add or update automated tests for
the behavior changed and run the relevant Gradle checks; changes MUST pass the
repository CI test command (`./gradlew test --no-daemon`) before merge. Follow
the stricter checks in `CONTRIBUTING.md` when preparing a pull request, and
update the changelog and operational documentation when the release checklist
requires them.

## Governance

This constitution records project-wide engineering guardrails and is used by
feature specifications, plans, implementation, and review. Amend it through a
reviewable change that explains the reason and scope of the amendment. Version
the constitution using semantic versioning: increment MAJOR for incompatible
governance changes, MINOR for new or materially expanded principles, and PATCH
for clarifications that do not change intent. Reviews MUST check proposed work
against these principles and explain any necessary exception in the relevant
plan or review.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): confirm original adoption date | **Last Amended**: 2026-10-16
