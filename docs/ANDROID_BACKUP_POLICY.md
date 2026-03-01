# Android Backup Policy

## Default Policy
- Release builds disable Android backup/restore (`android:allowBackup="false"`).
- Debug builds allow backups for developer convenience, but still exclude all app data by default.
- Explicit extraction rules live in `apps/control-kmp/androidApp/src/main/res/xml/data_extraction_rules.xml` and
  `apps/control-kmp/androidApp/src/main/res/xml/backup_rules.xml`.

## Rationale
The app handles tokens, logs, and potential PHI/PII payloads. Backups are disabled for production to avoid
unintentional data exfiltration or cross-device restores of sensitive state.

## Enterprise/MDM Overrides
For on-prem or MDM-controlled deployments that require backup support:
1. Create a dedicated product flavor or manifest overlay with `android:allowBackup="true"`.
2. Replace or refine the extraction rules to explicitly include only non-sensitive files.
3. Validate with the client security team before enabling backups in any regulated environment.

## Suggested Allowlist Strategy
If backups are required, prefer an allowlist approach (include only non-sensitive UI preferences), and keep
credentials, telemetry caches, evidence bundles, logs, and databases excluded.
