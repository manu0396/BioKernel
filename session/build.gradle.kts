plugins {
    id("neogenesis.biokernel.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    id("neogenesis.biokernel.android.common")
}

android {
    namespace = "com.neogenesis.session"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("demo") {
            dimension = "environment"
        }
        create("prod") {
            dimension = "environment"
        }
    }
}

dependencies {
    implementation(project(":data-core"))
    implementation(project(":domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.security.crypto)
    coreLibraryDesugaring(libs.desugar.jdk)
}






