package org.example.service

import org.example.DB
import org.example.Database
import org.example.dto.DishDTO

/**
 * Тип блюда
 *
 * Описывает не только к какому приёму пищи может принадлежать блюдо,
 * но и некоторые свойства этого типа для работы сервиса ([DishService]) и генератора рационов ([Decider][org.example.decider.Decider])
 *
 * @param defaultMinLimit минимальное значение количества выбираемых блюд этого типа. (по умолчанию)
 * @param defaultMaxLimit максимально значение количества выбираемых блюд этого типа. (по умолчанию)
 */
enum class DishType(val defaultMinLimit: Int, val defaultMaxLimit: Int) {
    BREAKFAST(5, 10),
    LUNCH(5, 10),
    DINNER(5, 10),
    LUNCH_OR_DINNER(3, 6),
    ANY(4, 7);

    /**
     * Получить количество блюд в таблице с указанным типом
     */
    fun getTypeCount(): Int {
        DishService

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

/**
 * Сервис, работающий с таблицей блюд в ClickHouse
 */
object DishService {

    init {
        Database.initService("V0_0_3__INIT_CH.sql", DB.CH)
    }

    /**
     * Получить блюда определенного типа
     *
     * Может получить блюда из указанного отрезка
     *
     * @param type тип блюд
     * @param offset начало отрезка
     * @param limit длина отрезка
     * @return набор прочитанных блюд
     */
    fun getDishesByType(
        type: DishType,
        offset: Int = 0,
        limit: Int = 10
    ): List<DishDTO> {
        val connection = Database.getCHConnection()

        val statement = connection.prepareStatement(
            "SELECT * FROM nutrition.dish WHERE type = ? ORDER BY type LIMIT ? OFFSET ?;",
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

    /**
     * Получить блюдо с указанным id
     */
    fun getDishById(dishId: Long): DishDTO {
        val connection = Database.getCHConnection()

        val statement = connection.prepareStatement("SELECT * FROM nutrition.dish WHERE id = ? LIMIT 1;")
        statement.setLong(1, dishId)

        val result = statement.executeQuery().use { rs ->
            if (rs.next())
                DishDTO(
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
            else throw Exception("No dish with such ID")
        }

        statement.close()
        connection.close()
        return result
    }
}
