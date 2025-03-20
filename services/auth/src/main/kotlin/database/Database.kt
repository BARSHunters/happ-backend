package database

import io.github.cdimascio.dotenv.dotenv
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import kotlin.system.exitProcess

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
            exitProcess(1)
        }
    }


    fun getConnection(): Connection {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    }


    private fun initializeDatabase() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                createUsersTable(statement)
                createTokenTable(statement)
            }
        }
    }

    private fun createUsersTable(statement: Statement) {
        val sql = """
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(255) PRIMARY KEY,
                password VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """.trimIndent()
        statement.execute(sql)
    }

    private fun createTokenTable(statement: Statement) {
        val sql = """
            CREATE TABLE IF NOT EXISTS tokens (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            token TEXT UNIQUE NOT NULL,
            token_type VARCHAR(50) NOT NULL,
            revoked BOOLEAN DEFAULT FALSE,
            expired_at TIMESTAMP NOT NULL,
            user_username VARCHAR(255) NOT NULL REFERENCES users(username) ON DELETE CASCADE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """.trimIndent()
        statement.execute(sql)
    }
}
