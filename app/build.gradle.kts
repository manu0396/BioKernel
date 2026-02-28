plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.neurogenesis.biokernel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neurogenesis.biokernel"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // UI & Navigation
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.navigation)

    // DI (Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Persistence Driver (Required for DI initialization)
    implementation(libs.sqldelight.android.driver)

    // Persistence Driver (Required for DI initialization in AppModule)
    implementation(libs.sqldelight.android.driver)

    // Networking
    implementation(libs.bundles.ktor)

    // Architecture Modules
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":data-core"))
    implementation(project(":components"))
    implementation(project(":session"))
    implementation(project(":feature-login"))
    implementation(project(":feature-dashboard"))
}