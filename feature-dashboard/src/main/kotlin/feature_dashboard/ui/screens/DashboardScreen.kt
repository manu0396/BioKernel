package feature_dashboard.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neogenesis.components.theme.BioKernelTheme
import com.neogenesis.components.widgets.RetinaCard
import com.neogenesis.feature_dashboard.R
import feature_dashboard.contract.DashboardIntent
import feature_dashboard.ui.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BioKernelTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("BioKernel Telemetry") },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            ) { padding ->
                Box(modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()) {
                    if (state.results.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(items = state.results, key = { it.id }) { analysis ->
                                RetinaCard(
                                    record = analysis,
                                    onClick = {
                                        if (!state.isLoading) {
                                            viewModel.handleIntent(
                                                DashboardIntent.SelectRecord(
                                                    analysis
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    } else if (!state.isLoading && state.error == null) {
                        Text(
                            text = "No telemetry data found.",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            if (!state.isLoading) {
                FloatingActionButton(
                    onClick = { viewModel.handleIntent(DashboardIntent.LoadRetinaData) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .zIndex(5f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload")
                }
            }

            if (state.isLoading) {
                BioKernelLoader(
                    modifier = Modifier.zIndex(100f)
                )
            }
        }

        state.selectedRecord?.let { record ->
            AlertDialog(
                onDismissRequest = { viewModel.handleIntent(DashboardIntent.DismissDialog) },
                title = { Text("Analysis Detail") },
                text = {
                    Column {
                        Text("ID: ${record.id}", style = MaterialTheme.typography.labelLarge)
                        Text("Hash: ${record.rawHash}")
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(record.notes)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.handleIntent(DashboardIntent.DismissDialog) }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun BioKernelLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader_transition")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loader_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loader_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Loading...",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .graphicsLayer { this.alpha = alpha }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Synchronizing BioKernel...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}