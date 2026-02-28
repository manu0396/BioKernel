package com.neogenesis.platform.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neogenesis.platform.desktop.storage.DesktopTokenStorage
import com.neogenesis.platform.proto.v1.TelemetryRequest
import com.neogenesis.platform.proto.v1.TelemetryStreamServiceGrpcKt
import com.neogenesis.platform.shared.domain.PrintJob
import com.neogenesis.platform.shared.domain.PrintJobStatus
import com.neogenesis.platform.shared.domain.Recipe
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.BillingApi
import com.neogenesis.platform.shared.network.EntitlementsRepository
import com.neogenesis.platform.shared.network.FeatureFlag
import com.neogenesis.platform.shared.network.HttpClientFactory
import com.neogenesis.platform.shared.network.KtorAuthApi
import com.neogenesis.platform.shared.network.KtorBillingApi
import com.neogenesis.platform.shared.network.KtorPrintJobApi
import com.neogenesis.platform.shared.network.KtorRecipeApi
import com.neogenesis.platform.shared.network.NetworkConfig
import com.neogenesis.platform.shared.network.NetworkError
import com.neogenesis.platform.shared.network.UpgradeUiState
import com.neogenesis.platform.shared.network.allowCleartextForLocalhost
import com.neogenesis.platform.shared.network.requiresPaywall
import com.neogenesis.platform.shared.network.toUpgradeUiState
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@Composable
fun RootScreen() {
    val scope = rememberCoroutineScope()
    val tokenStorage = remember { DesktopTokenStorage() }
    var baseUrl by remember { mutableStateOf("https://localhost:8443") }
    var grpcHost by remember { mutableStateOf("localhost") }
    var grpcPort by remember { mutableStateOf("9090") }
    var statusMessage by remember { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(tokenStorage.readAccessToken() != null) }
    var activeScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var paywallFeature by remember { mutableStateOf<FeatureFlag?>(null) }
    val client = remember(baseUrl) {
        HttpClientFactory.create(
            NetworkConfig(
                baseUrl = baseUrl,
                allowCleartext = allowCleartextForLocalhost(baseUrl)
            ),
            tokenStorage
        )
    }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    val authApi = remember(client) { KtorAuthApi(client, tokenStorage) }
    val recipeApi = remember(client) { KtorRecipeApi(client) }
    val jobApi = remember(client) { KtorPrintJobApi(client) }
    val billingApi: BillingApi = remember(client) { KtorBillingApi(client) }
    val entitlementsRepository = remember(client) { EntitlementsRepository(billingApi) }
    val entitlementsState by entitlementsRepository.state.collectAsState()

    val recipes = remember { mutableStateListOf<Recipe>() }
    val jobs = remember { mutableStateListOf<PrintJob>() }

    var telemetryJob by remember { mutableStateOf<Job?>(null) }
    var telemetryChannel by remember { mutableStateOf<ManagedChannel?>(null) }
    val telemetryFrames = remember { mutableStateListOf<com.neogenesis.platform.proto.v1.TelemetryFrame>() }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            entitlementsRepository.onAppStart()
        }
    }

    fun stopTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = null
        telemetryChannel?.shutdownNow()
        telemetryChannel = null
    }

    fun startTelemetry(jobId: String, deviceId: String, rateMs: Int) {
        stopTelemetry()
        telemetryFrames.clear()
        val port = grpcPort.toIntOrNull() ?: return
        val channel = ManagedChannelBuilder.forAddress(grpcHost, port).usePlaintext().build()
        telemetryChannel = channel
        telemetryJob = scope.launch {
            val stub = TelemetryStreamServiceGrpcKt.TelemetryStreamServiceCoroutineStub(channel)
            val request = TelemetryRequest.newBuilder()
                .setJobId(jobId)
                .setDeviceId(deviceId)
                .setRateMs(rateMs)
                .build()
            try {
                val responses = stub.streamTelemetry(
                    flow {
                        emit(request)
                        awaitCancellation()
                    }
                )
                responses.collect { frame ->
                    telemetryFrames.add(frame)
                    if (telemetryFrames.size > 50) {
                        telemetryFrames.removeAt(0)
                    }
                }
            } catch (ex: Exception) {
                statusMessage = "Telemetry stream error: ${ex.message ?: "unknown"}"
            } finally {
                channel.shutdownNow()
            }
        }
    }

    fun requireFeature(flag: FeatureFlag, onAllowed: () -> Unit) {
        if (entitlementsState.requiresPaywall(flag)) {
            paywallFeature = flag
            statusMessage = "Subscription required for ${flag.wireName}"
        } else {
            onAllowed()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("NeoGenesis Platform", style = MaterialTheme.typography.headlineSmall)
            if (!isAuthenticated) {
                LoginPanel(
                    baseUrl = baseUrl,
                    onBaseUrlChange = { baseUrl = it },
                    onLogin = { username, password ->
                        scope.launch {
                            statusMessage = "Signing in..."
                            when (val result = authApi.login(username, password)) {
                                is ApiResult.Success -> {
                                    isAuthenticated = true
                                    activeScreen = AppScreen.DASHBOARD
                                    statusMessage = "Signed in"
                                }
                                is ApiResult.Failure -> {
                                    statusMessage = errorMessage(result.error, "Login failed")
                                }
                            }
                        }
                    },
                    onRegister = { username, password ->
                        scope.launch {
                            statusMessage = "Registering..."
                            when (val result = authApi.register(username, password)) {
                                is ApiResult.Success -> {
                                    statusMessage = "User created. Sign in."
                                }
                                is ApiResult.Failure -> {
                                    statusMessage = errorMessage(result.error, "Registration failed")
                                }
                            }
                        }
                    }
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("HTTP Base URL") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { activeScreen = AppScreen.DASHBOARD }) {
                        Text("Dashboard")
                    }
                    Button(onClick = { activeScreen = AppScreen.UPGRADE }) {
                        Text("Account / Upgrade")
                    }
                    Button(onClick = {
                        scope.launch {
                            statusMessage = "Signing out..."
                            tokenStorage.readRefreshToken()?.let { authApi.logout(it) }
                            tokenStorage.clear()
                            isAuthenticated = false
                            paywallFeature = null
                            activeScreen = AppScreen.DASHBOARD
                            stopTelemetry()
                            statusMessage = "Signed out"
                        }
                    }) {
                        Text("Logout")
                    }
                }

                when (activeScreen) {
                    AppScreen.DASHBOARD -> DashboardPanels(
                        recipes = recipes,
                        jobs = jobs,
                        onRefreshRecipes = {
                            scope.launch {
                                when (val result = recipeApi.list()) {
                                    is ApiResult.Success -> {
                                        recipes.clear()
                                        recipes.addAll(result.value)
                                    }
                                    is ApiResult.Failure -> {
                                        statusMessage = errorMessage(result.error, "Failed to load recipes")
                                    }
                                }
                            }
                        },
                        onCreateRecipe = { name, description, params ->
                            scope.launch {
                                when (val result = recipeApi.create(name, description, params)) {
                                    is ApiResult.Success -> {
                                        recipes.add(0, result.value)
                                    }
                                    is ApiResult.Failure -> {
                                        statusMessage = errorMessage(result.error, "Recipe creation failed")
                                    }
                                }
                            }
                        },
                        onActivateRecipe = { id, active ->
                            scope.launch {
                                when (val result = recipeApi.activate(id, active)) {
                                    is ApiResult.Success -> {
                                        recipes.replaceAll { if (it.id.value == id) it.copy(active = active) else it }
                                    }
                                    is ApiResult.Failure -> {
                                        statusMessage = errorMessage(result.error, "Recipe update failed")
                                    }
                                }
                            }
                        },
                        onRefreshJobs = {
                            scope.launch {
                                when (val result = jobApi.list()) {
                                    is ApiResult.Success -> {
                                        jobs.clear()
                                        jobs.addAll(result.value)
                                    }
                                    is ApiResult.Failure -> {
                                        statusMessage = errorMessage(result.error, "Failed to load jobs")
                                    }
                                }
                            }
                        },
                        onCreateJob = { deviceId, operatorId, bioinkId, params ->
                            requireFeature(FeatureFlag.MULTI_DEVICE) {
                                scope.launch {
                                    when (val result = jobApi.create(deviceId, operatorId, bioinkId, params)) {
                                        is ApiResult.Success -> {
                                            jobs.add(0, result.value)
                                        }
                                        is ApiResult.Failure -> {
                                            statusMessage = errorMessage(result.error, "Job creation failed")
                                        }
                                    }
                                }
                            }
                        },
                        onUpdateJobStatus = { jobId, status ->
                            scope.launch {
                                when (val result = jobApi.updateStatus(jobId, status)) {
                                    is ApiResult.Success -> {
                                        jobs.replaceAll { if (it.id.value == jobId) it.copy(status = status) else it }
                                    }
                                    is ApiResult.Failure -> {
                                        statusMessage = errorMessage(result.error, "Job update failed")
                                    }
                                }
                            }
                        },
                        grpcHost = grpcHost,
                        grpcPort = grpcPort,
                        onGrpcHostChange = { grpcHost = it },
                        onGrpcPortChange = { grpcPort = it },
                        onStartTelemetry = { jobId, deviceId, rateMs ->
                            requireFeature(FeatureFlag.ADVANCED_TELEMETRY_EXPORT) {
                                startTelemetry(jobId, deviceId, rateMs)
                            }
                        },
                        onStopTelemetry = { stopTelemetry() },
                        telemetryFrames = telemetryFrames
                    )

                    AppScreen.UPGRADE -> UpgradePanel(
                        uiState = entitlementsState.toUpgradeUiState(),
                        onRefresh = {
                            scope.launch {
                                entitlementsRepository.onReturnedFromBrowserFlow()
                                statusMessage = "Billing status refreshed"
                            }
                        },
                        onSubscribe = {
                            scope.launch {
                                when (val result = billingApi.createCheckoutSession()) {
                                    is ApiResult.Success -> {
                                        if (openExternalBrowser(result.value)) {
                                            statusMessage = "Checkout opened in browser. Return to app and refresh status."
                                        } else {
                                            statusMessage = "Unable to open browser"
                                        }
                                    }
                                    is ApiResult.Failure -> {
                                        statusMessage = "Unable to start checkout session"
                                    }
                                }
                            }
                        },
                        onManage = {
                            scope.launch {
                                when (val result = billingApi.createPortalSession()) {
                                    is ApiResult.Success -> {
                                        if (openExternalBrowser(result.value)) {
                                            statusMessage = "Portal opened in browser. Return to app and refresh status."
                                        } else {
                                            statusMessage = "Unable to open browser"
                                        }
                                    }
                                    is ApiResult.Failure -> {
                                        statusMessage = "Unable to open subscription portal"
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (statusMessage.isNotBlank()) {
                Text(statusMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    paywallFeature?.let { feature ->
        AlertDialog(
            onDismissRequest = { paywallFeature = null },
            title = { Text("Premium Feature") },
            text = { Text("${feature.wireName} requires an active subscription.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        activeScreen = AppScreen.UPGRADE
                        paywallFeature = null
                    }
                ) {
                    Text("Upgrade")
                }
            },
            dismissButton = {
                TextButton(onClick = { paywallFeature = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun LoginPanel(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = baseUrl, onValueChange = onBaseUrlChange, label = { Text("HTTP Base URL") })
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onLogin(username, password) }) { Text("Login") }
                Button(onClick = { onRegister(username, password) }) { Text("Register") }
            }
        }
    }
}

@Composable
private fun UpgradePanel(
    uiState: UpgradeUiState,
    onRefresh: () -> Unit,
    onSubscribe: () -> Unit,
    onManage: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Upgrade", style = MaterialTheme.typography.titleLarge)
            Text("Plan: ${uiState.planLabel}")
            Text("Status: ${uiState.statusLabel}")
            Text("Renews: ${uiState.renewalLabel}")
            Text("Entitlements: ${uiState.features.filter { it.enabled }.joinToString { it.feature.wireName }.ifBlank { "none" }}")
            Text("Features", style = MaterialTheme.typography.titleMedium)
            uiState.features.forEach { item ->
                Text("- ${item.feature.wireName}: ${if (item.enabled) "enabled" else "locked"}")
            }
            if (uiState.showUnavailableBanner) {
                Text("Billing status unavailable. Showing last known data when possible.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSubscribe) { Text("Subscribe") }
                Button(onClick = onManage) { Text("Manage subscription") }
                Button(onClick = onRefresh) { Text("Refresh status") }
            }
        }
    }
}

@Composable
private fun DashboardPanels(
    recipes: List<Recipe>,
    jobs: List<PrintJob>,
    onRefreshRecipes: () -> Unit,
    onCreateRecipe: (String, String?, Map<String, String>) -> Unit,
    onActivateRecipe: (String, Boolean) -> Unit,
    onRefreshJobs: () -> Unit,
    onCreateJob: (String, String, String, Map<String, String>) -> Unit,
    onUpdateJobStatus: (String, PrintJobStatus) -> Unit,
    grpcHost: String,
    grpcPort: String,
    onGrpcHostChange: (String) -> Unit,
    onGrpcPortChange: (String) -> Unit,
    onStartTelemetry: (String, String, Int) -> Unit,
    onStopTelemetry: () -> Unit,
    telemetryFrames: List<com.neogenesis.platform.proto.v1.TelemetryFrame>
) {
    var recipeName by remember { mutableStateOf("") }
    var recipeDescription by remember { mutableStateOf("") }
    var recipeParams by remember { mutableStateOf("pressure=120\nflow=5") }

    var deviceId by remember { mutableStateOf("") }
    var operatorId by remember { mutableStateOf("") }
    var bioinkBatchId by remember { mutableStateOf("") }
    var jobParams by remember { mutableStateOf("speed=1.0") }
    var updateJobId by remember { mutableStateOf("") }
    var updateJobStatus by remember { mutableStateOf(PrintJobStatus.RUNNING) }

    var telemetryJobId by remember { mutableStateOf("") }
    var telemetryDeviceId by remember { mutableStateOf("") }
    var telemetryRateMs by remember { mutableStateOf("10") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Live Telemetry (gRPC)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = grpcHost, onValueChange = onGrpcHostChange, label = { Text("gRPC Host") })
                OutlinedTextField(value = grpcPort, onValueChange = onGrpcPortChange, label = { Text("gRPC Port") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = telemetryJobId, onValueChange = { telemetryJobId = it }, label = { Text("Job ID") })
                OutlinedTextField(value = telemetryDeviceId, onValueChange = { telemetryDeviceId = it }, label = { Text("Device ID") })
                OutlinedTextField(value = telemetryRateMs, onValueChange = { telemetryRateMs = it }, label = { Text("Rate ms") })
                Button(onClick = { onStartTelemetry(telemetryJobId, telemetryDeviceId, telemetryRateMs.toIntOrNull() ?: 10) }) {
                    Text("Start")
                }
                Button(onClick = onStopTelemetry) { Text("Stop") }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                items(telemetryFrames) { frame ->
                    Text("t=${frame.timestampMs} p=${frame.pressureKpa} kPa flow=${frame.flowRateUlS}")
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Recipes", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onRefreshRecipes) { Text("Refresh") }
            }
            OutlinedTextField(value = recipeName, onValueChange = { recipeName = it }, label = { Text("Name") })
            OutlinedTextField(value = recipeDescription, onValueChange = { recipeDescription = it }, label = { Text("Description") })
            OutlinedTextField(value = recipeParams, onValueChange = { recipeParams = it }, label = { Text("Params (key=value)") })
            Button(onClick = { onCreateRecipe(recipeName, recipeDescription.ifBlank { null }, parseParameters(recipeParams)) }) {
                Text("Create Recipe")
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                items(recipes) { recipe ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${recipe.name} (${if (recipe.active) "active" else "inactive"})")
                        Button(onClick = { onActivateRecipe(recipe.id.value, !recipe.active) }) {
                            Text(if (recipe.active) "Deactivate" else "Activate")
                        }
                    }
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Print Jobs", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onRefreshJobs) { Text("Refresh") }
            }
            OutlinedTextField(value = deviceId, onValueChange = { deviceId = it }, label = { Text("Device ID") })
            OutlinedTextField(value = operatorId, onValueChange = { operatorId = it }, label = { Text("Operator ID") })
            OutlinedTextField(value = bioinkBatchId, onValueChange = { bioinkBatchId = it }, label = { Text("Bioink Batch ID") })
            OutlinedTextField(value = jobParams, onValueChange = { jobParams = it }, label = { Text("Params (key=value)") })
            Button(onClick = { onCreateJob(deviceId, operatorId, bioinkBatchId, parseParameters(jobParams)) }) {
                Text("Create Job")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = updateJobId, onValueChange = { updateJobId = it }, label = { Text("Job ID") })
                OutlinedTextField(
                    value = updateJobStatus.name,
                    onValueChange = { value ->
                        PrintJobStatus.values().firstOrNull { it.name.equals(value, ignoreCase = true) }
                            ?.let { updateJobStatus = it }
                    },
                    label = { Text("Status") }
                )
                Button(onClick = { onUpdateJobStatus(updateJobId, updateJobStatus) }) { Text("Update Status") }
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                items(jobs) { job ->
                    Text("${job.id.value} :: ${job.status}")
                }
            }
        }
    }
}

private fun parseParameters(input: String): Map<String, String> {
    return input.lineSequence()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@mapNotNull null
            val parts = trimmed.split("=", limit = 2)
            if (parts.size < 2) return@mapNotNull null
            parts[0].trim() to parts[1].trim()
        }
        .toMap()
}

private fun errorMessage(
    error: NetworkError,
    fallback: String
): String {
    return when (error) {
        is NetworkError.HttpError -> error.message
        is NetworkError.SerializationError -> error.message
        is NetworkError.ConnectivityError -> error.message
        is NetworkError.TimeoutError -> error.message
        is NetworkError.UnknownError -> error.message
    }.ifBlank { fallback }
}

private fun openExternalBrowser(url: String): Boolean {
    return runCatching {
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            false
        } else {
            desktop.browse(URI(url))
            true
        }
    }.getOrDefault(false)
}

private enum class AppScreen {
    DASHBOARD,
    UPGRADE
}
