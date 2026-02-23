plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

val agpVersion = "8.5.2"
val kotlinVersion = "2.0.10"

dependencies {
    compileOnly("com.android.tools.build:gradle:$agpVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src/main/kotlin"))
        }
    }
}

gradlePlugin {
    plugins {
        register("androidBase") {
            id = "neogenesis.biokernel.android.base"
            implementationClass = "com.neogenesis.buildsrc.BaseAndroidConventionPlugin"
        }
        register("androidApplication") {
            id = "neogenesis.biokernel.android.application"
            implementationClass = "com.neogenesis.buildsrc.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "neogenesis.biokernel.android.library"
            implementationClass = "com.neogenesis.buildsrc.AndroidLibraryConventionPlugin"
        }
        register("androidCommon") {
            id = "neogenesis.biokernel.android.common"
            implementationClass = "com.neogenesis.buildsrc.AndroidCommonConventionPlugin"
        }
    }
}
