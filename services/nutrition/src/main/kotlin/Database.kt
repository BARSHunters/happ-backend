package org.example

import com.zaxxer.hikari.*
import java.sql.Connection

enum class DB {PG, CH}

object Database {
    private var chDataSource: HikariDataSource = HikariDataSource()
    private var pgDataSource: HikariDataSource = HikariDataSource()

    init {
        pgDataSource.driverClassName = "org.postgresql.Driver"
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

    fun initService(migrationFileName: String, db: DB) {
        // Загружаем файл миграции этого сервиса
        val sqlScript = this::class.java.classLoader.getResourceAsStream(migrationFileName)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("`$migrationFileName` file not found")

        sqlScript.split(";")
            .filter { it.isNotBlank() }
            .forEach { sql ->
                when (db) {
                    DB.CH -> getCHConnection()
                    DB.PG -> getPGConnection()
                }.createStatement().use { statement ->
                    statement.execute(sql.trim())
                }
            }
    }
}