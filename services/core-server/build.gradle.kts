import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    alias(libs.plugins.protobuf)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("com.neogenesis.platform.core.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("core-server")
    archiveClassifier.set("all")
    archiveVersion.set("")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    val main by getting {
        kotlin.srcDir("../../agents/device-gateway/src/main/kotlin")
    }
    val test by getting {
        kotlin.srcDir("../../agents/device-gateway/src/test/kotlin")
    }
}

dependencies {
    implementation(project(":shared:proto"))
    implementation(project(":apps:control-kmp:shared"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikari)
    implementation(libs.postgres)

    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin)
    implementation(libs.protobuf.java)
    implementation(libs.neogenesis.contracts)

    implementation(libs.logback)
    implementation(libs.logstash)
    implementation(libs.micrometer.prometheus)
    implementation(libs.otel.api)
    implementation(libs.otel.sdk)
    implementation(libs.otel.sdk.trace)
    implementation(libs.otel.exporter.otlp)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.flyway)
    implementation(libs.jbcrypt)
    implementation(libs.identity.jvm)

    testImplementation(kotlin("test"))
    testImplementation(libs.grpc.inprocess)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.serialization)
    testImplementation(libs.h2)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        id("grpc") {
            artifact = libs.grpc.protoc.get().toString()
        }
        id("grpckt") {
            artifact = "${libs.grpc.kotlin.protoc.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins { id("grpc") }
            task.plugins { id("grpckt") }
        }
    }
}

