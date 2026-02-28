plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "com.neogenesis.data"
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

sqldelight {
    databases {
        create("BioKernelDatabase") {
            packageName.set("com.neogenesis.data.db")
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data-core"))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    api(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.runtime)
    implementation(libs.sqldelight.coroutines.extensions)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}



