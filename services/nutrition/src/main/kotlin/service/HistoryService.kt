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
                        rs.getDouble("total_tdee"),
                        rs.getDouble("total_protein"),
                        rs.getDouble("total_fat"),
                        rs.getDouble("total_carbs")
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
        statement.setInt(3, dishSet.breakfast.weight.toInt())
        statement.setLong(4, dishSet.lunch.id ?: 0)
        statement.setInt(5, dishSet.lunch.weight.toInt())
        statement.setLong(6, dishSet.dinner.id ?: 0)
        statement.setInt(7, dishSet.dinner.weight.toInt())
        statement.setDouble(8, dishSet.tdee)
        statement.setDouble(9, dishSet.protein)
        statement.setDouble(10, dishSet.fat)
        statement.setDouble(11, dishSet.carbs)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }
}