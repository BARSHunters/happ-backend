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
                        (username, name, birth_day, gender, heightCm, weightKg, weightDesire) 
                        VALUES (?, ?, ?, ?, ?, ?, ?);
                        """.trimIndent()
                )
                statement.setString(1, userData.username)
                statement.setString(2, userData.name)
                statement.setTimestamp(3, Timestamp.valueOf(userData.birthDay.toString()))
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
                        birthDay = rs.getTimestamp("birth_day").toLocalDateTime().toLocalDate(),
                        gender = Gender.valueOf(rs.getString("gender")),
                        heightCm = rs.getInt("heightCm"),
                        weightKg = rs.getFloat("weightKg"),
                        weightDesire = WeightDesire.valueOf(rs.getString("weightDesire"))
                    )
                } else null
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            null
        }
    }

//    fun updateUserData(userData: UserData): Boolean{
//        return try {
//            Database.getConnection().use { connection ->
//                connection.prepareStatement("""
//                    UPDATE
//                """.trimIndent())
//            }
//        }
//    }
}