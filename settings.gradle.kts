pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NeoGenesisPlatform"

include(":domain")
include(":data")
include(":shared-network")
include(":androidApp")
include(":desktopApp")
include(":backend")
include(":firmware-protocol")
