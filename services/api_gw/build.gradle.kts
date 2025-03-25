
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ktlint)
}

group = "happ-backend"
version = "1.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
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
