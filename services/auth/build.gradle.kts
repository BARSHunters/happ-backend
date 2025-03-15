

plugins {
    kotlin("jvm") version "2.0.10"
    kotlin("plugin.serialization") version "2.0.10"
}

group = "kotlin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.postgresql:postgresql:42.5.6")
    implementation("at.favre.lib:bcrypt:0.9.0")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}