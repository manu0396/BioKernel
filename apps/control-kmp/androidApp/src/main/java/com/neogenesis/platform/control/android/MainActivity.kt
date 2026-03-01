package com.neogenesis.platform.control.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.di.initKoin
import com.neogenesis.platform.control.platform.androidPlatformModule
import com.neogenesis.platform.control.presentation.RegenOpsApp
import com.neogenesis.platform.control.presentation.RegenOpsViewModel
import com.neogenesis.platform.control.presentation.design.NgTheme
import org.koin.core.Koin
import org.koin.core.component.KoinComponent

class MainActivity : ComponentActivity(), KoinComponent {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resolvedHttpBaseUrl = resolveHttpBaseUrl(BuildConfig.REGENOPS_HTTP_BASE_URL)
        val appConfig = AppConfig(
            httpBaseUrl = resolvedHttpBaseUrl,
            grpcHost = BuildConfig.REGENOPS_GRPC_HOST,
            grpcPort = BuildConfig.REGENOPS_GRPC_PORT,
            grpcUseTls = BuildConfig.REGENOPS_GRPC_TLS,
            oidcIssuer = BuildConfig.OIDC_ISSUER,
            oidcClientId = BuildConfig.OIDC_CLIENT_ID,
            oidcAudience = BuildConfig.OIDC_AUDIENCE.takeIf { it.isNotBlank() },
            tenantId = BuildConfig.TENANT_ID,
            traceModeEnabled = BuildConfig.TRACE_MODE,
            demoModeEnabled = BuildConfig.DEMO_MODE,
            founderModeEnabled = BuildConfig.FOUNDER_MODE,
            commercialModeEnabled = BuildConfig.COMMERCIAL_MODE
        )
        val koin: Koin = initKoin(appConfig, androidPlatformModule(this, appConfig))
        val viewModel: RegenOpsViewModel = koin.get()

        setContent {
            NgTheme {
                RegenOpsApp(
                    viewModel = viewModel,
                    openExternalUrl = { url ->
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        }
                    },
                    shareFile = { bytes, fileName, mimeType ->
                        val file = java.io.File(cacheDir, fileName)
                        file.writeBytes(bytes)
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            this,
                            "${packageName}.fileprovider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share export"))
                    }
                )
            }
        }
    }

    private fun resolveHttpBaseUrl(baseUrl: String): String {
        val normalized = baseUrl.trim()
        if (!isEmulator()) return normalized
        return normalized
            .replace("http://localhost", "http://10.0.2.2")
            .replace("http://127.0.0.1", "http://10.0.2.2")
    }

    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            "google_sdk" == Build.PRODUCT
    }
}
