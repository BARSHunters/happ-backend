plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.clickhouse:clickhouse-jdbc:0.8.2:shaded-all")
    implementation("com.zaxxer:HikariCP:6.2.1")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}