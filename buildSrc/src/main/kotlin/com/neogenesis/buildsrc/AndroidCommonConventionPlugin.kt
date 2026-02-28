package com.neogenesis.buildsrc

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class AndroidCommonConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 1. Toolchain (Uses JDK 21 for speed)
            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(21)
            }

            // 2. ANDROID CONFIGURATION (Reactive)
            // We use 'com.android.base' which covers both Library and Application plugins.
            // This ensures we don't configure it before the Android plugin is loaded.
            pluginManager.withPlugin("com.android.base") {
                extensions.configure<BaseExtension> {
                    compileOptions {
                        sourceCompatibility = JavaVersion.VERSION_17
                        targetCompatibility = JavaVersion.VERSION_17
                    }
                }
            }

            // 3. KOTLIN TASKS (Force 17)
            tasks.withType<KotlinCompile>().configureEach {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }

            // 4. JAVA TASKS (Safety Override)
            // Explicitly force the tasks to 17, overriding any toolchain defaults.
            tasks.withType<JavaCompile>().configureEach {
                sourceCompatibility = JavaVersion.VERSION_17.toString()
                targetCompatibility = JavaVersion.VERSION_17.toString()
            }
        }
    }
}