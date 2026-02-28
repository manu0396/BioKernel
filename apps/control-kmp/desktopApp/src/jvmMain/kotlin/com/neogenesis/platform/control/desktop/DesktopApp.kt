package com.neogenesis.platform.control.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.di.initKoin
import com.neogenesis.platform.control.platform.desktopPlatformModule
import com.neogenesis.platform.control.presentation.RegenOpsApp
import com.neogenesis.platform.control.presentation.RegenOpsViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.awt.Desktop
import java.net.URI

object DesktopApp : KoinComponent {
    @JvmStatic
    fun main(args: Array<String>) = application {
        val appConfig = AppConfig(
            httpBaseUrl = System.getenv("REGENOPS_HTTP_BASE_URL") ?: "http://localhost:8080",
            grpcHost = System.getenv("REGENOPS_GRPC_HOST") ?: "localhost",
            grpcPort = (System.getenv("REGENOPS_GRPC_PORT") ?: "9090").toInt(),
            grpcUseTls = (System.getenv("REGENOPS_GRPC_TLS") ?: "false").toBoolean(),
            oidcIssuer = System.getenv("OIDC_ISSUER") ?: "",
            oidcClientId = System.getenv("OIDC_CLIENT_ID") ?: "",
            oidcAudience = System.getenv("OIDC_AUDIENCE")
        )
        initKoin(appConfig, desktopPlatformModule(appConfig))
        val viewModel: RegenOpsViewModel = get()

        Window(onCloseRequest = ::exitApplication, title = "RegenOps Control") {
            MaterialTheme {
                RegenOpsApp(
                    viewModel = viewModel,
                    openExternalUrl = { url ->
                        runCatching { Desktop.getDesktop().browse(URI(url)) }
                    },
                    shareFile = { bytes, fileName, _ ->
                        val file = java.io.File(System.getProperty("user.home"), fileName)
                        file.writeBytes(bytes)
                        runCatching { Desktop.getDesktop().open(file) }
                    }
                )
            }
        }
    }
}
