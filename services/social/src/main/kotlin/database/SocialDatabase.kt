package database

import io.github.cdimascio.dotenv.dotenv
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.system.exitProcess

object SocialDatabase {
    private val dotenv = dotenv()
    private val dbUrl = dotenv["DB_URL"]
    private val dbUser = dotenv["DB_USER"]
    private val dbPassword = dotenv["DB_PASSWORD"]

    init {
        try {
            createTables()
        } catch (e: Exception) {
            println("Database connection error: ${e.message}")
            exitProcess(1)
        }
    }

    private fun createTables() {
        getConnection().use { conn ->
            try {
                val statement = conn.createStatement()
                statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS friendships (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        sender_username VARCHAR(255) NOT NULL,
                        receiver_username VARCHAR(255) NOT NULL,
                        status VARCHAR(50) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(sender_username, receiver_username)
                    );
                    """
                )
                println("Social service tables created successfully")
            } catch (e: SQLException) {
                println("Error creating tables: ${e.message}")
                throw e
            }
        }
    }

    fun getConnection(): Connection {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    }
} 