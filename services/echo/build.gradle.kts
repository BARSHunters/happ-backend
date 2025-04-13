plugins {
    kotlin("jvm") version "2.0.10"
    application
}

group = "io.github.barshunters"
version = "1.0-SNAPSHOT"

application {
    mainClass = "ServiceMainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}