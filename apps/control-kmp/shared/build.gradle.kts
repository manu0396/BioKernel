import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(21)

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
    
    // Rename to jvm() to match jvmMain source set naming conventions
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            // Collapse shared Gradle modules into this KMP module for IDE stability
            kotlin.srcDir("../../../shared/domain/src/commonMain/kotlin")
            kotlin.srcDir("../../../shared/data/src/commonMain/kotlin")
            kotlin.srcDir("../../../shared/network/src/commonMain/kotlin")
            dependencies {
                api(project(":shared:proto"))
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.serialization)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)
                implementation(libs.koin.core)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
        val commonTest by getting {
            kotlin.srcDir("../../../shared/domain/src/commonTest/kotlin")
            kotlin.srcDir("../../../shared/data/src/commonTest/kotlin")
            kotlin.srcDir("../../../shared/network/src/commonTest/kotlin")
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            kotlin.srcDir("../../../shared/domain/src/androidMain/kotlin")
            kotlin.srcDir("../../../shared/data/src/androidMain/kotlin")
            kotlin.srcDir("../../../shared/network/src/androidMain/kotlin")
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.grpc.okhttp)
                implementation(libs.grpc.kotlin)
                implementation(libs.protobuf.java)
                implementation(libs.neogenesis.contracts)
                implementation(libs.androidx.security.crypto)
                implementation(libs.koin.android)
            }
        }
        val jvmMain by getting {
            kotlin.srcDir("../../../shared/domain/src/desktopMain/kotlin")
            kotlin.srcDir("../../../shared/data/src/desktopMain/kotlin")
            kotlin.srcDir("../../../shared/network/src/desktopMain/kotlin")
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.grpc.netty)
                implementation(libs.grpc.kotlin)
                implementation(libs.grpc.protobuf)
                implementation(libs.grpc.stub)
                implementation(libs.protobuf.java)
                implementation(libs.neogenesis.contracts)
            }
        }
    }
}

android {
    namespace = "com.neogenesis.platform.control.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

sqldelight {
    databases {
        create("RegenOpsDatabase") {
            packageName.set("com.neogenesis.platform.control.data.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
