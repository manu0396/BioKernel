plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.neogenesis.biokernel"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neogenesis.biokernel"
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

    //SQLDelight
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.android.driver)

    // Networking
    implementation(libs.bundles.ktor)

    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // Architecture Modules
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":data-core"))
    implementation(project(":components"))
    implementation(project(":session"))
    implementation(project(":feature-login"))
    implementation(project(":feature-dashboard"))
}



