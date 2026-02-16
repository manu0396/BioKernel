plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.neogenesis.datacore"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    flavorDimensions += "environment"
    productFlavors {
        create("demo") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://mock.api\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.biokernel.neogenesis.com\"")
        }
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
    implementation(libs.ktor.client.mock)
    implementation(libs.kotlinx.serialization.json)
}



