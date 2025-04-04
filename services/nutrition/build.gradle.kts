plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.0.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
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
    implementation("com.clickhouse:clickhouse-jdbc:0.8.2")
    implementation("com.zaxxer:HikariCP:6.2.1")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    implementation("ch.qos.logback:logback-classic:1.4.14")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

configurations {
    all {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

// Конфигурация ktlint
ktlint {
    version.set("1.0.1") // Укажите версию ktlint
    debug.set(true) // Включите отладочный вывод
    verbose.set(true) // Включите подробный вывод
    outputToConsole.set(true) // Вывод результатов в консоль
    ignoreFailures.set(false) // Остановить сборку при обнаружении ошибок
    enableExperimentalRules.set(true) // Включите экспериментальные правила
    filter {
        exclude("**/generated/**") // Исключите сгенерированные файлы
    }
}