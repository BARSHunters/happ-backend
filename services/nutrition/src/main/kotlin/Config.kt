package org.example

import io.github.cdimascio.dotenv.dotenv
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Конфигурация приложения
 *
 * При инициализации этого объекта происходит поиск `application.properties` файла.
 *
 * Есть поддержка профилей запуска. Для этого надо указать в переменной окружения `PROFILE` названия профиля.
 * Тогда, если в ресурсах есть файл `application-<PROFILE>.properties`, то будет применен он.
 * Если его нет - будет выброшен [IllegalStateException]
 *
 * Есть поддержка указания переменных окружения в `.properties` файлах.
 * Если значение переменной в нем указано как `${VAR_NAME}`, то приложение попробует найти переменную окружения `VAR_NAME`
 * (и в окружении, и в файле `.env`).
 * При неудаче будет выброшен [IllegalStateException]
 *
 * Есть поддержка `.env` фалов. Если переменная не найдена в окружении, то приложение попробует найти ее в этом файле.
 * Если не найдена нигде - будет выброшен [IllegalStateException]
 * Если `.env` файл не найден стадия поиска в нем пропускается.
 */
object Config {
    private val logger: Logger = LoggerFactory.getLogger(Database::class.java)
    private val env = dotenv {
        directory = "./services/nutrition" // Указываем путь к .env файлу
        ignoreIfMissing = true // Игнорировать, если файл отсутствует
    }

    private val properties = Properties()

    init {
        var profile = System.getenv("PROFILE") ?: ""

        if (System.getProperty("test") == "true") {
            profile = "test"
        }

        logger.info("Using '$profile'")
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
            System.getenv(envVarName) ?: env[envVarName] // TODO `env[envVarName]` не выкинет исключение? Вроде нет.
            ?: throw IllegalStateException("Environment variable $envVarName not found")
        }
    }

    /**
     * Получить значение свойства конфигурации приложения
     *
     * @param key имя свойства
     */
    fun getProperty(key: String): String {
        return properties.getProperty(key) ?: throw IllegalStateException("Property $key not found")
    }
}