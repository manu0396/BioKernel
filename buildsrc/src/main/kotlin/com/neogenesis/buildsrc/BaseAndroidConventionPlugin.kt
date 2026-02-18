package com.neogenesis.buildsrc

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class BaseAndroidConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.android")

            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(21)
            }
        }
    }
}