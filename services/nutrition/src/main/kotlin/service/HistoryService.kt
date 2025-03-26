package org.example.service

import org.example.Database
import org.example.dto.DailyDishSetDTO
import org.example.dto.HistoryRow

object HistoryService {
    fun getHistoryForUser(login: String, days: Int): List<Pair<String, HistoryRow>> {
        val connection = Database.getPGConnection()

        val statement = connection.prepareStatement(
            "SELECT * FROM history WHERE login = ? AND date >= NOW() - INTERVAL ? DAY"
        )
        statement.setString(1, login)
        statement.setInt(2, days)

        val result = statement.executeQuery().use { rs ->
            buildList {
                while (rs.next()) {
                    this += rs.getDate("date").toString() to HistoryRow(
                        rs.getInt("total_tdee").toDouble(),
                        rs.getInt("total_protein").toDouble(),
                        rs.getInt("total_fat").toDouble(),
                        rs.getInt("total_carbs").toDouble()
                    )
                }
            }
        }
        statement.close()
        connection.close()
        return result
    }

    fun addHistory(login: String, dishSet: DailyDishSetDTO) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement(
            """INSERT INTO history VALUES
            |(?,now(),?,?,?,?,?,?,?,?,?,?)
        """.trimMargin()
        )
        statement.setString(1, login)
        statement.setLong(2, dishSet.breakfast.id ?: 0)
        statement.setLong(3, dishSet.breakfast.weight.toLong())
        statement.setLong(4, dishSet.lunch.id ?: 0)
        statement.setLong(5, dishSet.lunch.weight.toLong())
        statement.setLong(6, dishSet.dinner.id ?: 0)
        statement.setLong(7, dishSet.dinner.weight.toLong())
        statement.setInt(8, dishSet.tdee.toInt())
        statement.setInt(9, dishSet.protein.toInt())
        statement.setInt(10, dishSet.fat.toInt())
        statement.setInt(11, dishSet.carbs.toInt())
        statement.executeUpdate()
        statement.close()
        connection.close()
    }
}