package org.example

import com.zaxxer.hikari.*
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import java.sql.Connection

enum class DB { PG, CH }

/**
 * Объект для работы с базами данных
 *
 * Хранит pool-ы подключений.
 *
 * Умеет запускать указанные sql скрипты из ресурсов (попытка сделать что-то похожее на "миграции в ручную")
 */
object Database {
    private val logger: Logger = LoggerFactory.getLogger(Database::class.java)
    private var chDataSource: HikariDataSource = HikariDataSource()
    private var pgDataSource: HikariDataSource = HikariDataSource()

    init {
        pgDataSource.maximumPoolSize = Config.getProperty("pg.maxPoolSize").toInt()
        pgDataSource.username = Config.getProperty("pg.user")
        pgDataSource.password = Config.getProperty("pg.password")
        pgDataSource.jdbcUrl = Config.getProperty("pg.jdbcUrl")

        chDataSource.maximumPoolSize = Config.getProperty("ch.maxPoolSize").toInt()
        chDataSource.username = Config.getProperty("ch.user")
        chDataSource.password = Config.getProperty("ch.password")
        chDataSource.jdbcUrl = Config.getProperty("ch.jdbcUrl")
    }

    fun getCHConnection(): Connection = chDataSource.connection ?: throw ExceptionInInitializerError("Database connection is null")
    fun getPGConnection(): Connection = pgDataSource.connection ?: throw ExceptionInInitializerError("Database connection is null")

    /**
     * Запускает указанный sql скрипт из ресурсов приложения.
     *
     * Также проверит по таблице migrations, что он ещё не применялся.
     *
     * Если этой таблицы нет - создаст.
     */
    fun initService(migrationFileName: String, db: DB) {
        // Загружаем файл миграции этого сервиса
        try {
            val conn = when (db) {
                DB.CH -> getCHConnection()
                DB.PG -> getPGConnection()
            }

            if (checkIfExists(migrationFileName, conn, db)) {
                logger.info("migration $migrationFileName already applied.")
                conn.close()
                return
            }

            val sqlScript = this::class.java.classLoader.getResourceAsStream("db/migration/$migrationFileName")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw IllegalStateException("`$migrationFileName` file not found")

            if (db == DB.PG) conn.autoCommit = false

            sqlScript.split(";")
                .filter { it.isNotBlank() }
                .forEach { sql ->
                    conn.createStatement().use { statement ->
                        statement.execute(sql.trim())
                    }
                }

            conn.prepareStatement("INSERT INTO nutrition.migrations VALUES (?);").use {
                it.setString(1, migrationFileName)
                it.executeUpdate()
            }

            if (db == DB.PG)  conn.commit()
            conn.close()
            logger.info("NEW migration $migrationFileName applied!")
        } catch (e: Exception) {
            println(e.message)
            throw e
        }
    }

    private fun checkIfExists(migrationFileName: String, conn: Connection, db: DB): Boolean {
        val createMigrationTable = when (db) {
            DB.CH -> "CREATE table IF NOT EXISTS nutrition.migrations (file_name VARCHAR(255) not null) ENGINE = TinyLog;"
            DB.PG -> "CREATE table IF NOT EXISTS nutrition.migrations (file_name VARCHAR(255) not null);"
        }

        conn.prepareStatement(createMigrationTable).use {
            it.executeUpdate()
        }
        val st = conn.prepareStatement("SELECT COUNT(*) FROM nutrition.migrations WHERE file_name = ?")
        st.setString(1, migrationFileName)

        val res = st.executeQuery()
        return (res.next() && res.getInt(1) >= 1). also {
            st.close()
        }
    }
}