package repository

import database.Database
import model.User

class UserRepository {
    // TODO добавить логику добавления пользователя в бд
    fun createUser(username: String, password: String): Boolean{
        return try {
            Database.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    "INSERT INTO users (username,password) VALUES (?, ?);"
                )
                statement.setString(1, username)
                statement.setString(2, password)
                statement.executeUpdate() > 0
            }
        } catch (e: Exception){
            println("DB Error: ${e.message}")
            false
        }
    }

    // TODO добавить логику нахождения пользователя в бд
    fun findUserByUsername(username: String): User? {
        return Database.getConnection().use { connection ->
            val statement = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ?;"
            )
            statement.setString(1, username)
            val rs = statement.executeQuery()
            if (rs.next()){
                User(
                    username = rs.getString("username"),
                    password = rs.getString("password")
                )
            } else null
        }
    }
}