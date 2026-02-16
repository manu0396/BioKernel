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

    flavorDimensions += "environment"
    productFlavors {
        create("demo") {
            dimension = "environment"
            resValue("string", "app_name", "BioKernel (Demo)")
        }
        create("prod") {
            dimension = "environment"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Android Standard Libs
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM & Bundle
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Módulos del Proyecto
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":data-core"))
    implementation(project(":feature-dashboard"))
    implementation(project(":feature-login"))
    implementation(project(":session"))
    implementation(project(":components"))

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    implementation(libs.androidx.compose.navigation)
    implementation(libs.bundles.ktor)
    implementation(libs.sqldelight.android.driver)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Testing UI
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}