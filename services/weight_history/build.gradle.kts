plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

group = "happ-backend"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("WeightHistoryServiceKt")
}

dependencies {
    implementation(project(":shared"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("io.mockk:mockk:1.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
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
