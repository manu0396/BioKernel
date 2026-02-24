import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

allprojects {
    group = "com.neogenesis.platform"
    version = "1.0.0"

    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

subprojects {
    val strictQuality = providers.gradleProperty("strictQuality")
        .map { it.toBooleanStrictOrNull() == true }
        .orElse(providers.environmentVariable("CI").map { true }.orElse(false))

    val applyQualityPlugins: () -> Unit = {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") { applyQualityPlugins() }
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") { applyQualityPlugins() }
    pluginManager.withPlugin("org.jetbrains.kotlin.android") { applyQualityPlugins() }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom("$rootDir/config/detekt/detekt.yml")
            ignoreFailures = !strictQuality.get()
        }
        tasks.withType<Detekt>().configureEach {
            exclude("**/build/**")
            exclude("**/generated/**")
            exclude("**/.gradle/**")
        }
    }

    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            ignoreFailures.set(!strictQuality.get())
            outputToConsole.set(false)
            filter {
                exclude("**/build/**")
                exclude("**/generated/**")
                exclude("**/.gradle/**")
                exclude("**/*.kts")
            }
        }
    }

    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
            allWarningsAsErrors.set(
                providers.gradleProperty("warningsAsErrors")
                    .map { it.toBooleanStrictOrNull() == true }
                    .orElse(false)
            )
        }
    }
}

tasks.register<Exec>("runSimulator") {
    commandLine("python", "tools/device_simulator.py")
}
