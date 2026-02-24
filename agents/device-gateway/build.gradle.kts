plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("com.neogenesis.platform.gateway.DeviceGatewayKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    archiveBaseName.set("device-gateway")
    archiveVersion.set("")
}

dependencies {
    implementation(project(":shared:proto"))
    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.datetime)
    implementation(libs.logback)
    implementation(libs.logstash)
    testImplementation(kotlin("test"))
}
