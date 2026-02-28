plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compiler.embeddable)
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