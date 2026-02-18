plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation(libs.bundles.ktor.server)
}

application {
    mainClass.set("com.neogenesis.mock.MockServerKt")
}


