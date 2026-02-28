import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.setSrcDirs(listOf("src/commonMain/kotlin"))
            dependencies {
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines)
                implementation(libs.koin.core)
            }
        }
        val commonTest by getting {
            kotlin.setSrcDirs(listOf("src/commonTest/kotlin"))
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines)
            }
        }
        val jvmSharedMain by creating {
            dependsOn(commonMain)
        }
        val androidMain by getting {
            kotlin.setSrcDirs(listOf("src/androidMain/kotlin"))
            dependsOn(jvmSharedMain)
        }
        val desktopMain by getting {
            dependsOn(jvmSharedMain)
        }
    }
}

android {
    namespace = "com.neogenesis.platform.shared"
    compileSdk = 34
    buildToolsVersion = "35.0.0"
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

