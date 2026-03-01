package com.neogenesis.platform.control.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.di.initKoin
import com.neogenesis.platform.control.platform.desktopPlatformModule
import com.neogenesis.platform.control.presentation.RegenOpsApp
import com.neogenesis.platform.control.presentation.RegenOpsViewModel
import org.koin.core.Koin
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.Properties

fun main() = application {
    println("--- BioKernel Control Startup ---")
    println("Working Directory: ${System.getProperty("user.dir")}")
    
    val props = loadConfig()
    
    val appConfig = AppConfig(
        httpBaseUrl = getSetting(props, "REGENOPS_HTTP_BASE_URL", "http://localhost:8080"),
        grpcHost = getSetting(props, "REGENOPS_GRPC_HOST", "localhost"),
        grpcPort = getSetting(props, "REGENOPS_GRPC_PORT", "9090").toInt(),
        grpcUseTls = getSetting(props, "REGENOPS_GRPC_TLS", "false").toBoolean(),
        oidcIssuer = getSetting(props, "OIDC_ISSUER", ""),
        oidcClientId = getSetting(props, "OIDC_CLIENT_ID", ""),
        oidcAudience = getSetting(props, "OIDC_AUDIENCE", null),
        tenantId = getSetting(props, "TENANT_ID", "tenant-1"),
        traceModeEnabled = getSetting(props, "TRACE_MODE", "false").toBoolean(),
        demoModeEnabled = getSetting(props, "DEMO_MODE", "false").toBoolean(),
        founderModeEnabled = getSetting(props, "FOUNDER_MODE", "false").toBoolean(),
        commercialModeEnabled = getSetting(props, "COMMERCIAL_MODE", "false").toBoolean()
    )
    
    println("Configured OIDC Issuer: ${appConfig.oidcIssuer.takeIf { it.isNotBlank() } ?: "MISSING"}")
    
    val koin: Koin = initKoin(appConfig, desktopPlatformModule(appConfig))
    val viewModel: RegenOpsViewModel = koin.get()

    Window(onCloseRequest = ::exitApplication, title = "RegenOps Control") {
        MaterialTheme {
            RegenOpsApp(
                viewModel = viewModel,
                openExternalUrl = { url ->
                    runCatching { Desktop.getDesktop().browse(URI(url)) }
                },
                shareFile = { bytes, fileName, _ ->
                    val file = File(System.getProperty("user.home"), fileName)
                    file.writeBytes(bytes)
                    runCatching { Desktop.getDesktop().open(file) }
                }
            )
        }
    }
}

private fun loadConfig(): Properties {
    val props = Properties()
    
    // Search for local.properties or .env in multiple potential root locations
    val searchDirectories = listOf(
        File("."),
        File(".."),
        File("../.."),
        File("../../..")
    )
    
    val targetFiles = listOf("local.properties", ".env")
    
    searchDirectories.forEach { dir ->
        targetFiles.forEach { fileName ->
            val file = File(dir, fileName)
            if (file.exists()) {
                println("Found config file: ${file.absolutePath}")
                file.inputStream().use { props.load(it) }
            }
        }
    }
    
    return props
}

private fun getSetting(props: Properties, key: String, default: String?): String {
    return System.getenv(key) 
        ?: props.getProperty(key) 
        ?: props.getProperty(key.lowercase())
        ?: props.getProperty(key.lowercase().replace("_", "."))
        ?: default 
        ?: ""
}
