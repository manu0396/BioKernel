plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

dependencies {
    implementation(project(":shared-network"))
    implementation(libs.bundles.ktor.server)
}

application {
    mainClass.set("com.neurogenesis.backend.ApplicationKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "com.neurogenesis.backend.ApplicationKt"
    }
}