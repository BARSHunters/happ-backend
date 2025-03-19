package database

import io.github.cdimascio.dotenv.dotenv
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

object Database {
    private val dotenv = dotenv()
    private val dbUrl = dotenv["DB_URL"]
    private val dbUser = dotenv["DB_USER"]
    private val dbPassword = dotenv["DB_PASSWORD"]

    init {
        try {
            initializeDatabase()
        } catch (e: SQLException) {
            println("Error initializing database: ${e.message}")
            e.printStackTrace()
        }
    }


    fun getConnection(): Connection {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    }


    private fun initializeDatabase() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                createUsersDataTable(statement)
            }
        }
    }

    private fun createUsersDataTable(statement: Statement) {
        val sql = """
            CREATE TABLE IF NOT EXISTS users_data (
            username VARCHAR(255) PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            birth_date DATE NOT NULL,
            gender VARCHAR(50) NOT NULL,
            height_cm INT CHECK (height_cm > 0),
            weight_kg REAL CHECK (weight_kg > 0),
            weight_desire VARCHAR(50) NOT NULL);
        """.trimIndent()
        statement.execute(sql)
    }
}