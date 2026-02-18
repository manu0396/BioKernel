plugins {
    id("neogenesis.biokernel.android.library")
    id("org.jetbrains.kotlin.android")
    id("neogenesis.biokernel.android.common")
}

android {
    namespace = "com.neogenesis.domain"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.android)
}






