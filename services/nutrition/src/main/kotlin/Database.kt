package org.example

import com.zaxxer.hikari.*
import java.sql.Connection

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
}