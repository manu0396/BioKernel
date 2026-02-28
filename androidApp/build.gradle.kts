import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProperties = Properties().apply {
    val propertiesFile = file("keystore/keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

val storeFilePath = System.getenv("NEOGENESIS_KEYSTORE_PATH")
    ?: keystoreProperties.getProperty("storeFile")
val storePassword = System.getenv("NEOGENESIS_KEYSTORE_PASSWORD")
    ?: keystoreProperties.getProperty("storePassword")
val keyAlias = System.getenv("NEOGENESIS_KEY_ALIAS")
    ?: keystoreProperties.getProperty("keyAlias")
val keyPassword = System.getenv("NEOGENESIS_KEY_PASSWORD")
    ?: keystoreProperties.getProperty("keyPassword")
val releaseSigningRequired = System.getenv("RELEASE_SIGNING_REQUIRED")?.toBooleanStrictOrNull() == true
val hasReleaseSigning = !storeFilePath.isNullOrBlank() &&
    !storePassword.isNullOrBlank() &&
    !keyAlias.isNullOrBlank() &&
    !keyPassword.isNullOrBlank()

android {
    namespace = "com.neogenesis.platform.android"
    compileSdk = 34
    buildToolsVersion = "35.0.0"

    signingConfigs {
        create("release") {
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
            }
            this.storePassword = storePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }

    defaultConfig {
        applicationId = "com.neogenesis.platform.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        checkReleaseBuilds = false
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

tasks.register("verifyReleaseSigning") {
    doLast {
        if (releaseSigningRequired && !hasReleaseSigning) {
            val missing = mutableListOf<String>()
            if (System.getenv("NEOGENESIS_KEYSTORE_PATH").isNullOrBlank() && !file("keystore/keystore.properties").exists()) {
                missing.add("NEOGENESIS_KEYSTORE_PATH or androidApp/keystore/keystore.properties")
            }
            if (System.getenv("NEOGENESIS_KEYSTORE_PASSWORD").isNullOrBlank() && keystoreProperties.getProperty("storePassword").isNullOrBlank()) {
                missing.add("NEOGENESIS_KEYSTORE_PASSWORD")
            }
            if (System.getenv("NEOGENESIS_KEY_ALIAS").isNullOrBlank() && keystoreProperties.getProperty("keyAlias").isNullOrBlank()) {
                missing.add("NEOGENESIS_KEY_ALIAS")
            }
            if (System.getenv("NEOGENESIS_KEY_PASSWORD").isNullOrBlank() && keystoreProperties.getProperty("keyPassword").isNullOrBlank()) {
                missing.add("NEOGENESIS_KEY_PASSWORD")
            }
            error("Missing release signing config: ${missing.joinToString(", ")}")
        }
    }
}

listOf("bundleRelease", "assembleRelease").forEach { taskName ->
    tasks.matching { it.name == taskName }.configureEach {
        dependsOn("verifyReleaseSigning")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":shared-network"))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.security.crypto)
}

