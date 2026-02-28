@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "BioKernel"

// Shared modules
include(":shared")
include(":shared:proto")
include(":shared:data")
include(":shared:domain")
include(":shared:network")

// Services
include(":services")
include(":services:core-server")

// Agents
include(":agents")
include(":agents:device-gateway")

// Apps (KMP)
include(":apps")
include(":apps:control-kmp")
include(":apps:control-kmp:shared")
include(":apps:control-kmp:androidApp")
include(":apps:control-kmp:desktopApp")
