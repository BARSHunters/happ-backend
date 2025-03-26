package org.example.service

import org.example.Database
import org.example.dto.DishDTO

enum class DishType(val defaultMinLimit: Int, val defaultMaxLimit: Int) {
    BREAKFAST(5, 10),
    LUNCH(5, 10),
    DINNER(5, 10),
    LUNCH_OR_DINNER(3, 6),
    ANY(4, 7);

    fun getTypeCount(): Int {
        val connection = Database.getCHConnection()

        val statement = connection.prepareStatement(
            "SELECT COUNT(*) FROM dish WHERE type = ?",
        )
        statement.setString(1, this.name)

        val result = statement.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }

        statement.close()
        connection.close()
        return result
    }
}

object DishService {
    fun getDishesByType(
        type: DishType,
        offset: Int = 0,
        limit: Int = 10
    ): List<DishDTO> {
        val connection = Database.getCHConnection()

        val statement = connection.prepareStatement(
            "SELECT * FROM dish WHERE type = ? ORDER BY type LIMIT ? OFFSET ?;",
        )
        statement.setString(1, type.toString())
        statement.setInt(2, offset)
        statement.setInt(3, limit)

        val result = statement.executeQuery().use { rs ->
            buildList {
                while (rs.next()) {
                    this += DishDTO(
                        rs.getString("name"),
                        100U,
                        rs.getInt("tdee").toDouble(),
                        rs.getInt("protein").toDouble(),
                        rs.getInt("fat").toDouble(),
                        rs.getInt("carbs").toDouble(),
                        rs.getLong("id"),
                        rs.getLong("photoId"),
                        rs.getLong("recipeId")
                    )
                }
            }
        }

        statement.close()
        connection.close()
        return result
    }
}

// TODO реализовать в сервисах что-то +- около миграций на файлах из ресурсов?