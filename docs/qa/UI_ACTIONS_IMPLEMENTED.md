# UI Actions Implemented

**Protocols**
| Control | Expected behavior | Implementation file | Backend endpoint | Demo/mock support | Test coverage |
| --- | --- | --- | --- | --- | --- |
| Search field | Filters protocol list in-memory as you type. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt | N/A (local filter) | MockProtocols sample when backend empty. | Manual |
| Refresh | Re-fetch protocols and update list with status banner. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `GET /api/v1/regenops/protocols` | DemoControlApi + MockProtocols. | Unit: RegenOpsViewModelTest |
| Protocol card | Selects protocol and navigates to detail. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt | N/A (state + navigation) | DemoControlApi + MockProtocols. | Manual |

**Protocol Detail**
| Control | Expected behavior | Implementation file | Backend endpoint | Demo/mock support | Test coverage |
| --- | --- | --- | --- | --- | --- |
| Version card | Selects a version for runs/publish. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt | N/A (state) | DemoControlApi + MockProtocols. | Manual |
| Publish button | Publishes selected version and shows status. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `POST /api/v1/regenops/protocols/{id}/publish` | DemoControlApi. | Manual |

**Run Control**
| Control | Expected behavior | Implementation file | Backend endpoint | Demo/mock support | Test coverage |
| --- | --- | --- | --- | --- | --- |
| Protocol Change | Opens dialog and sets selected protocol. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt | N/A (state) | MockProtocols. | Android: RunControlUiTest#versionPickerShowsListAndClose |
| Version Change | Opens dialog and sets selected version. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt | N/A (state) | MockProtocols. | Android: RunControlUiTest#versionPickerShowsListAndClose |
| Simulated Run toggle | Toggles simulation mode in UI state. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | N/A (state) | N/A | Android: RunControlUiTest#simulateChipToggles |
| SIMULATE/SIMULATION chip | Toggles simulation mode from header chip. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt | N/A (state) | N/A | Android: RunControlUiTest#simulateChipToggles |
| Start Mission | Starts run, shows loading, navigates to live. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `POST /api/v1/regenops/runs/start` | DemoControlApi. | Android: RunControlUiTest#startMissionShowsLoadingThenNavigates, Unit: RegenOpsViewModelTest#startRunNavigatesToLive |
| Configure Simulation | Opens simulation settings dialog. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt | N/A (state) | N/A | Android: RunControlUiTest#startSimulationShowsLoadingThenNavigates |
| Start Simulation | Starts simulator run, navigates to live. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `POST /demo/simulator/runs?tenant_id=...` | Demo simulator module (NG_DEMO_MODE=true). | Android: RunControlUiTest#startSimulationShowsLoadingThenNavigates, Unit: RegenOpsViewModelTest#startSimulatedRunNavigatesToLive |
| Demo | Starts demo run via startRun when demo mode enabled. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `POST /api/v1/regenops/runs/start` | DemoControlApi. | Manual |
| Pause | Pauses selected run with status update. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `POST /api/v1/regenops/runs/{id}/pause` | DemoControlApi. | Manual |
| Stop | Aborts selected run with status update. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `POST /api/v1/regenops/runs/{id}/abort` | DemoControlApi. | Manual |
| Run card | Selects run and opens live screen. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsApp.kt | N/A (state + navigation) | DemoControlApi. | Manual |
| Refresh Runs | Re-fetch run list. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `GET /api/v1/regenops/runs` | DemoControlApi. | Manual |

**Live Run**
| Control | Expected behavior | Implementation file | Backend endpoint | Demo/mock support | Test coverage |
| --- | --- | --- | --- | --- | --- |
| Pause | Pauses current run. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `POST /api/v1/regenops/runs/{id}/pause` | DemoControlApi. | Manual |
| Stop | Aborts current run. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `POST /api/v1/regenops/runs/{id}/abort` | DemoControlApi. | Manual |
| Download Report | Exports report bytes and share intent. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `GET /api/v1/telemetry/{runId}/export` (alias) | ExportsAliasModule. | Manual |

**Exports**
| Control | Expected behavior | Implementation file | Backend endpoint | Demo/mock support | Test coverage |
| --- | --- | --- | --- | --- | --- |
| Export Report | Downloads report bytes and shows status. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `GET /api/v1/telemetry/{runId}/export` (alias) | ExportsAliasModule. | Manual |
| Audit Bundle | Downloads audit bundle bytes and shows status. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `GET /api/v1/evidence/{runId}/package` (alias) | ExportsAliasModule. | Manual |

**Trace**
| Control | Expected behavior | Implementation file | Backend endpoint | Demo/mock support | Test coverage |
| --- | --- | --- | --- | --- | --- |
| Refresh | Loads reproducibility score and drift alerts. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `GET /api/v1/metrics/reproducibility-score`, `GET /api/v1/metrics/drift-alerts` | MetricsModule (NG_DEMO_MODE=true). | Server: DemoEndpointsTest |

**Commercial**
| Control | Expected behavior | Implementation file | Backend endpoint | Demo/mock support | Test coverage |
| --- | --- | --- | --- | --- | --- |
| Refresh | Loads pipeline stages and opportunities. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `GET /api/v1/commercial/pipeline` | CommercialModule (NG_DEMO_MODE=true). | Server: DemoEndpointsTest |
| Export CSV | Downloads CSV export. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | `GET /api/v1/commercial/pipeline/export` | CommercialModule (NG_DEMO_MODE=true). | Server: DemoEndpointsTest |
| Opportunity card | Opens detail dialog and allows close. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/Screens.kt | N/A (state) | N/A | Manual |

**Auth**
| Control | Expected behavior | Implementation file | Backend endpoint | Demo/mock support | Test coverage |
| --- | --- | --- | --- | --- | --- |
| Begin Authorization | Starts device auth flow and displays user code. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | OIDC Device Auth (external) | N/A | Manual |
| Open Verification URL | Opens external browser to verification URL. | apps/control-kmp/androidApp/src/main/java/com/neogenesis/platform/control/android/MainActivity.kt | N/A (platform action) | N/A | Manual |
| Confirm Authorization | Polls for tokens and transitions to authenticated state. | apps/control-kmp/shared/src/commonMain/kotlin/com/neogenesis/platform/control/presentation/RegenOpsViewModel.kt | OIDC Token Poll (external) | N/A | Manual |
