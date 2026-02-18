package com.neogenesis.buildsrc

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "neogenesis.biokernel.android.base")

            extensions.configure<ApplicationExtension> {
                defaultConfig.targetSdk = 35
                compileSdk = 35

                buildFeatures {
                    compose = true
                }
            }
        }
    }
}