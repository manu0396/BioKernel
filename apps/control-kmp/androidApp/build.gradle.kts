plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

val oidcIssuer = providers.gradleProperty("OIDC_ISSUER")
    .orElse(providers.environmentVariable("OIDC_ISSUER"))
    .orNull ?: ""

val oidcClientId = providers.gradleProperty("OIDC_CLIENT_ID")
    .orElse(providers.environmentVariable("OIDC_CLIENT_ID"))
    .orNull ?: ""

val oidcAudience = providers.gradleProperty("OIDC_AUDIENCE")
    .orElse(providers.environmentVariable("OIDC_AUDIENCE"))
    .orNull ?: ""

val regenopsHttpBaseUrl = providers.gradleProperty("REGENOPS_HTTP_BASE_URL")
    .orElse(providers.environmentVariable("REGENOPS_HTTP_BASE_URL"))
    .orNull ?: "http://10.0.2.2:8080"

val regenopsGrpcHost = providers.gradleProperty("REGENOPS_GRPC_HOST")
    .orElse(providers.environmentVariable("REGENOPS_GRPC_HOST"))
    .orNull ?: "10.0.2.2"

val regenopsGrpcPort = providers.gradleProperty("REGENOPS_GRPC_PORT")
    .orElse(providers.environmentVariable("REGENOPS_GRPC_PORT"))
    .orNull?.toIntOrNull() ?: 50051

val regenopsGrpcTls = providers.gradleProperty("REGENOPS_GRPC_TLS")
    .orElse(providers.environmentVariable("REGENOPS_GRPC_TLS"))
    .orNull?.toBoolean() ?: false

android {
    namespace = "com.neogenesis.platform.control.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neogenesis.platform.control.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OIDC_ISSUER", "\"$oidcIssuer\"")
        buildConfigField("String", "OIDC_CLIENT_ID", "\"$oidcClientId\"")
        buildConfigField("String", "OIDC_AUDIENCE", "\"$oidcAudience\"")
        buildConfigField("String", "REGENOPS_HTTP_BASE_URL", "\"$regenopsHttpBaseUrl\"")
        buildConfigField("String", "REGENOPS_GRPC_HOST", "\"$regenopsGrpcHost\"")
        buildConfigField("int", "REGENOPS_GRPC_PORT", "$regenopsGrpcPort")
        buildConfigField("boolean", "REGENOPS_GRPC_TLS", "$regenopsGrpcTls")
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
