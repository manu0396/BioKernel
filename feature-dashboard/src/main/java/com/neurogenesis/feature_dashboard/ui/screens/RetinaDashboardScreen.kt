package com.neurogenesis.feature_dashboard.ui.screens

// FIX: Essential import for koinViewModel in Compose
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neurogenesis.feature_dashboard.ui.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetinaDashboardScreen(
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Retina-on-a-Chip Dashboard") })
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier
                .padding(padding)
                .fillMaxSize()) {
                items(state.results) { analysis ->
                    Card(modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Sample ID: ${analysis.id}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                "Compatibility: ${analysis.compatibilityScore}%",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            // Aligned property 'toxicity'
                            Text(
                                text = "Toxicity Status: ${analysis.toxicity}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}