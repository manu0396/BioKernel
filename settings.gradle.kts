pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://repo.zetetic.net/public/")
            content { includeGroup("net.zetetic") }
        }
    }
}

rootProject.name = "BioKernel"

include(":app")
include(":data")
include(":data-core")
include(":domain")
include(":session")
include(":components")
include(":feature-login")
include(":feature-dashboard")
include(":server")
