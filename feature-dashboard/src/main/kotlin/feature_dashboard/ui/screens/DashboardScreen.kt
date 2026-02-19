package feature_dashboard.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neogenesis.components.theme.BioKernelTheme
import com.neogenesis.components.widgets.AnalysisDetailDialog
import com.neogenesis.components.widgets.EmptyStatePlaceholder
import com.neogenesis.components.widgets.RetinaCard
import com.neogenesis.data_core.persistence.BiometricAuthManager
import com.neogenesis.domain.model.ToxicityLevel
import com.neogenesis.feature_dashboard.R
import feature_dashboard.contract.DashboardIntent
import feature_dashboard.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private enum class AuthStatus {
    CHECKING,
    AUTHENTICATED,
    DENIED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = state.error?.asString()
    var selectedFilter by remember { mutableStateOf<ToxicityLevel?>(null) }
    var authStatus by remember { mutableStateOf(AuthStatus.AUTHENTICATED) }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val biometricManager: BiometricAuthManager = koinInject { parametersOf(activity) }

    val performAuth = {
        if (activity != null) {
            authStatus = AuthStatus.CHECKING
            activity.lifecycleScope.launch {
                activity.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    delay(300)
                    biometricManager.authenticateAndGetKey()
                        .onSuccess {
                            authStatus = AuthStatus.AUTHENTICATED
                            viewModel.handleIntent(DashboardIntent.InitializeAuthorizedSession)
                        }
                        .onFailure {
                            authStatus = AuthStatus.AUTHENTICATED // ROLLBACK
                            viewModel.handleIntent(DashboardIntent.LoadRetinaData) // ROLLBACK
                        }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        //performAuth()
        viewModel.handleIntent(DashboardIntent.LoadRetinaData)
    }

    BioKernelTheme {
        when (authStatus) {
            AuthStatus.CHECKING -> {
                BioKernelLoader()
            }
            AuthStatus.DENIED -> {
                AccessDeniedScreen(
                    onRetry = { performAuth() },
                    onExit = { activity?.finish() }
                )
            }
            AuthStatus.AUTHENTICATED -> {
                val hasLethalThreat = state.results.any { it.toxicity == ToxicityLevel.LETHAL }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(if (hasLethalThreat) "⚠️ BIOHAZARD ALERT" else "BioKernel Dashboard") },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = if (hasLethalThreat) Color(0xFFB71C1C) else MaterialTheme.colorScheme.primary,
                                titleContentColor = Color.White
                            )
                        )
                    },
                    floatingActionButton = {
                        if (!state.isLoading) {
                            FloatingActionButton(
                                onClick = { viewModel.handleIntent(DashboardIntent.LoadRetinaData) },
                                containerColor = if (hasLethalThreat) Color(0xFFB71C1C) else MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White,
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload")
                            }
                        }
                    }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                        ToxicityFilterBar(
                            selectedLevel = selectedFilter,
                            onLevelSelected = { level ->
                                selectedFilter = level
                                viewModel.filterBy(level)
                            }
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (errorMessage!= null) {
                                ErrorRetryView(
                                    message = errorMessage,
                                    onRetry = { viewModel.handleIntent(DashboardIntent.LoadRetinaData) },
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else if (state.results.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    items(items = state.results, key = { it.id }) { analysis ->
                                        val isLethal = analysis.toxicity == ToxicityLevel.LETHAL
                                        val itemModifier = if (isLethal) {
                                            Modifier.border(2.dp, Color.Red, RoundedCornerShape(12.dp))
                                        } else Modifier

                                        Box(modifier = itemModifier) {
                                            RetinaCard(record = analysis, onClick = {
                                                if (!state.isLoading) viewModel.handleIntent(DashboardIntent.SelectRecord(analysis))
                                            })
                                        }
                                    }
                                }
                            } else if (!state.isLoading) {
                                EmptyStatePlaceholder(modifier = Modifier.align(Alignment.Center))
                            }

                            if (state.isLoading) {
                                BioKernelLoader(modifier = Modifier.zIndex(100f))
                            }
                        }
                    }
                }

                state.selectedRecord?.let { record ->
                    AnalysisDetailDialog(
                        record = record,
                        onDismiss = { viewModel.handleIntent(DashboardIntent.DismissDialog) }
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToxicityFilterBar(selectedLevel: ToxicityLevel?, onLevelSelected: (ToxicityLevel?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        item {
            FilterChip(selected = selectedLevel == null, onClick = { onLevelSelected(null) }, label = { Text("All") })
        }
        items(ToxicityLevel.entries.toTypedArray()) { level ->
            FilterChip(selected = selectedLevel == level, onClick = { onLevelSelected(level) }, label = { Text(level.name) })
        }
    }
}

@Composable
private fun ErrorRetryView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Error Loading Data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Text(message, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun BioKernelLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = ""
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = ""
    )
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp).scale(scale).graphicsLayer { this.alpha = alpha }
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("Synchronizing BioKernel...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AccessDeniedScreen(onRetry: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("ACCESS DENIED", style = MaterialTheme.typography.headlineMedium, color = Color.Red)
        Text("Biometric decryption failed. Database remains locked.", color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("RETRY AUTHENTICATION") }
        TextButton(onClick = onExit) { Text("EXIT APPLICATION", color = Color.White) }
    }
}


