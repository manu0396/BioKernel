plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared:domain"))
                implementation(project(":shared:network"))
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
        val commonTest by getting
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.grpc.okhttp)
                implementation(libs.grpc.kotlin)
                implementation(libs.protobuf.java)
                implementation(libs.neogenesis.contracts)
                implementation(libs.koin.android)
            }
        }
        val desktopMain by getting {
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
}

sqldelight {
    databases {
        create("RegenOpsDatabase") {
            packageName.set("com.neogenesis.platform.control.data.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}

