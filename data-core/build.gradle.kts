plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.neurogenesis.datacore"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.bundles.ktor)
    implementation(libs.androidx.security.crypto)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
}