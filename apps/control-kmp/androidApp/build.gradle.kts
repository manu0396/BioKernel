import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Load local.properties manually from the project root
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun propertyOrEnv(key: String, default: String): String {
    val camelKey = key.lowercase().split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }.replaceFirstChar { it.lowercase() }
    
    return localProperties.getProperty(key)
        ?: localProperties.getProperty(camelKey)
        ?: (findProperty(key) as? String)
        ?: (findProperty(camelKey) as? String)
        ?: System.getenv(key)
        ?: default
}

fun propertyOrEnvInt(key: String, default: Int): Int {
    val value = propertyOrEnv(key, default.toString())
    return value.toIntOrNull() ?: default
}

fun propertyOrEnvBool(key: String, default: Boolean): Boolean {
    val value = propertyOrEnv(key, default.toString())
    return value.toBooleanStrictOrNull() ?: default
}

android {
    namespace = "com.neogenesis.platform.control.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neogenesis.platform.control.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Map configuration to BuildConfig
        buildConfigField("String", "OIDC_ISSUER", "\"${propertyOrEnv("OIDC_ISSUER", "")}\"")
        buildConfigField("String", "OIDC_CLIENT_ID", "\"${propertyOrEnv("OIDC_CLIENT_ID", "")}\"")
        buildConfigField("String", "OIDC_AUDIENCE", "\"${propertyOrEnv("OIDC_AUDIENCE", "")}\"")

        buildConfigField("String", "REGENOPS_HTTP_BASE_URL", "\"${propertyOrEnv("REGENOPS_HTTP_BASE_URL", "http://10.0.2.2:8080")}\"")
        buildConfigField("String", "REGENOPS_GRPC_HOST", "\"${propertyOrEnv("REGENOPS_GRPC_HOST", "10.0.2.2")}\"")
        buildConfigField("int", "REGENOPS_GRPC_PORT", "${propertyOrEnvInt("REGENOPS_GRPC_PORT", 50051)}")
        buildConfigField("boolean", "REGENOPS_GRPC_TLS", "${propertyOrEnvBool("REGENOPS_GRPC_TLS", false)}")

        buildConfigField("boolean", "TRACE_MODE", "${propertyOrEnvBool("TRACE_MODE", false)}")
        buildConfigField("boolean", "DEMO_MODE", "${propertyOrEnvBool("DEMO_MODE", false)}")
        buildConfigField("boolean", "FOUNDER_MODE", "${propertyOrEnvBool("FOUNDER_MODE", false)}")
        buildConfigField("boolean", "COMMERCIAL_MODE", "${propertyOrEnvBool("COMMERCIAL_MODE", false)}")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":apps:control-kmp:shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
