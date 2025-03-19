package org.example

import java.util.*
import io.github.cdimascio.dotenv.dotenv


object Config {
    private val env = dotenv {
        directory = "./services/nutrition" // Указываем путь к .env файлу
        ignoreIfMissing = true // Игнорировать, если файл отсутствует
    }

    private val properties = Properties()

    init {
        var profile = System.getenv("PROFILE") ?: ""
        System.out.println("Using '$profile'")
        profile = if (profile.isNotEmpty()) ("-$profile") else ""
        profile = profile.lowercase()

        // Загружаем файл config.properties
        this::class.java.classLoader.getResourceAsStream("application$profile.properties")?.use {
            properties.load(it)
        } ?: throw IllegalStateException("`application$profile.properties` file not found")

        // Подставляем значения переменных окружения
        properties.replaceAll { _, value ->
            replaceEnvVariables(value.toString())
        }
    }

    private fun replaceEnvVariables(value: String): String {
        return value.replace(Regex("\\$\\{(.*?)}")) { matchResult ->
            val envVarName = matchResult.groupValues[1]
            System.getenv(envVarName) ?: env[envVarName] ?: throw IllegalStateException("Environment variable $envVarName not found")
        }
    }

    fun getProperty(key: String): String {
        return properties.getProperty(key) ?: throw IllegalStateException("Property $key not found")
    }
}