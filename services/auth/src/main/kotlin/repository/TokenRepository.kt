package repository

import database.Database
import model.Token
import model.TokenType
import java.sql.Timestamp
import java.util.*

class TokenRepository {
    fun save(token: Token): Boolean {
        return try {
            Database.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    "INSERT INTO tokens (token, token_type, revoked, expired_at, user_username) VALUES (?, ?, ?, ?, ?);"
                )
                statement.setString(1, token.token)
                statement.setString(2, token.tokenType.toString())
                statement.setBoolean(3, token.revoked)
                statement.setTimestamp(4, Timestamp.valueOf(token.expiredAt))
                statement.setString(5, token.userUsername)
                statement.executeUpdate() > 0
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            false
        }
    }

    fun findByToken(token: String): Token? {
        return try {
            Database.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    """
                        SELECT * FROM tokens WHERE token = ?
                    """.trimIndent()
                )
                statement.setString(1, token)
                val rs = statement.executeQuery()
                if (rs.next()) {
                    Token(
                        id = UUID.fromString(rs.getString("id")),
                        token = rs.getString("token"),
                        tokenType = TokenType.valueOf(rs.getString("token_type")),
                        expiredAt = rs.getTimestamp("expired_at").toLocalDateTime(),
                        revoked = rs.getBoolean("revoked"),
                        userUsername = rs.getString("user_username")
                    )
                } else null
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            null
        }
    }

    fun revokeToken(token: Token): Boolean {
        return try {
            Database.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    """
                        UPDATE tokens SET revoked = ?
                        where token = ?;
                    """.trimIndent()
                )
                statement.setBoolean(1, token.revoked)
                statement.setString(2, token.token)
                statement.executeUpdate() > 0
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            false
        }
    }
}