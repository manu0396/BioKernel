plugins {
    id("neogenesis.biokernel.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
}

sqldelight {
    databases {
        create("BioKernelDatabase") {
            packageName.set("com.neogenesis.data.db")
        }
    }
}

android {
    namespace = "com.neogenesis.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(project(":domain"))
    implementation(project(":data-core"))
    implementation(project(":session"))
    implementation(project(":shared-network"))
    implementation(libs.koin.android)
    api(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.coroutines.extensions)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.workmanager)
    coreLibraryDesugaring(libs.desugar.jdk)
}


