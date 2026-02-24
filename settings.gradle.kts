pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "NeoGenesisPlatform"

include(":shared:domain")
include(":shared:data")
include(":shared:network")
include(":shared:proto")
include(":services:core-server")
include(":agents:device-gateway")

include(":apps:control-kmp:shared")
include(":apps:control-kmp:androidApp")
include(":apps:control-kmp:desktopApp")
