package repository

import database.Database
import model.Gender
import model.UserData
import model.WeightDesire
import java.sql.Timestamp

class UserRepository {
    fun createUser(userData: UserData): Boolean {
        return try {
            Database.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    """
                        INSERT INTO users_data 
                        (username, name, birth_date, gender, height_cm, weight_kg, weight_desire) 
                        VALUES (?, ?, ?, ?, ?, ?, ?);
                        """.trimIndent()
                )
                statement.setString(1, userData.username)
                statement.setString(2, userData.name)
                statement.setTimestamp(3, Timestamp.valueOf(userData.birthDate.toString()))
                statement.setString(4, userData.gender.toString())
                statement.setInt(5, userData.heightCm)
                statement.setFloat(6, userData.weightKg)
                statement.setString(7, userData.weightDesire.toString())
                statement.executeUpdate() > 0
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            false
        }
    }

    fun findByUsername(username: String): UserData? {
        return try {
            Database.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    """
                        SELECT * from users_data
                        WHERE username = ?;
                        """.trimIndent()
                )
                statement.setString(1, username)
                val rs = statement.executeQuery()
                if (rs.next()) {
                    UserData(
                        username = rs.getString("username"),
                        name = rs.getString("name"),
                        birthDate = rs.getTimestamp("birth_date").toLocalDateTime().toLocalDate(),
                        gender = Gender.valueOf(rs.getString("gender")),
                        heightCm = rs.getInt("height_cm"),
                        weightKg = rs.getFloat("weight_kg"),
                        weightDesire = WeightDesire.valueOf(rs.getString("weight_desire"))
                    )
                } else null
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            null
        }
    }

    @Suppress("SqlSourceToSinkFlow")
    fun updateUserFields(username: String, updatedFields: Map<String, Any?>): Boolean {
        if (updatedFields.isEmpty()) return false
        val setClauses = updatedFields.keys.joinToString(", ") { "$it = ?" }
        val query = "UPDATE users_data SET $setClauses WHERE username = ?"
        return try {
            Database.getConnection().use { connection ->
                connection.prepareStatement(query).use { statement ->
                    var index = 1
                    updatedFields.values.forEach { value ->
                        statement.setObject(index++, value)
                    }
                    statement.setString(index, username)
                    statement.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            false
        }
    }
}