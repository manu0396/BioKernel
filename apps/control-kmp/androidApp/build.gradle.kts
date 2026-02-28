plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.neogenesis.platform.control.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neogenesis.platform.control.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "REGENOPS_HTTP_BASE_URL", "\"${System.getenv("REGENOPS_HTTP_BASE_URL") ?: "http://10.0.2.2:8080"}\"")
        buildConfigField("String", "REGENOPS_GRPC_HOST", "\"${System.getenv("REGENOPS_GRPC_HOST") ?: "10.0.2.2"}\"")
        buildConfigField("int", "REGENOPS_GRPC_PORT", "${System.getenv("REGENOPS_GRPC_PORT") ?: "9090"}")
        buildConfigField("boolean", "REGENOPS_GRPC_TLS", "${System.getenv("REGENOPS_GRPC_TLS") ?: "false"}")
        buildConfigField("String", "OIDC_ISSUER", "\"${System.getenv("OIDC_ISSUER") ?: ""}\"")
        buildConfigField("String", "OIDC_CLIENT_ID", "\"${System.getenv("OIDC_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "OIDC_AUDIENCE", "\"${System.getenv("OIDC_AUDIENCE") ?: ""}\"")
        buildConfigField("boolean", "COMMERCIAL_MODE", "${System.getenv("COMMERCIAL_MODE") ?: "false"}")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":apps:control-kmp:shared"))
    implementation(libs.koin.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.security.crypto)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
