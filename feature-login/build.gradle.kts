plugins {
    id("neogenesis.biokernel.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.neogenesis.feature_login"
    flavorDimensions.add("environment")

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
    implementation(project(":components"))
    implementation(project(":session"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}


