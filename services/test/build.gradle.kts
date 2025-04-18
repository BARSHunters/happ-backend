plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.cio.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    testImplementation("io.ktor:ktor-client")
    implementation("io.ktor:ktor-client-cio")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
    testImplementation("junit:junit:4.13.2")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}