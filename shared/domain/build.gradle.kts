import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    jvmToolchain(21)

    androidTarget {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    jvm("desktop") {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
            }
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
            kotlin.setSrcDirs(listOf("src/jvmSharedMain/kotlin"))
        }

        val jvmSharedTest by creating {
            dependsOn(commonTest)
            kotlin.setSrcDirs(listOf("src/jvmSharedTest/kotlin"))
        }

        val androidMain by getting {
            dependsOn(jvmSharedMain)
            kotlin.setSrcDirs(listOf("src/androidMain/kotlin"))
        }

        val androidUnitTest by getting {
            dependsOn(jvmSharedTest)
        }

        val desktopMain by getting {
            dependsOn(jvmSharedMain)
            kotlin.setSrcDirs(listOf("src/desktopMain/kotlin"))
        }

        val desktopTest by getting {
            dependsOn(jvmSharedTest)
        }
    }
}

android {
    namespace = "com.neogenesis.platform.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}