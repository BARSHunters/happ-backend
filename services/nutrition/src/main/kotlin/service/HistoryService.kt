package org.example.service

import org.example.DB
import org.example.Database
import org.example.dto.DailyDishSetDTO
import org.example.dto.HistoryFullDTO
import org.example.dto.HistoryRow
import java.time.LocalDate

/**
 * Сервис, работающий с таблицей истории рационов пользователей в Postgres
 */
object HistoryService {

    init {
        Database.initService("V0_0_1__INIT_PG_HISTORY.sql", DB.PG)
    }

    /**
     * КБЖУ актуальных рационов за последние дни
     *
     * В таблице хранится вся история, включая повторные генерации. Больший id - более актуальная запись рациона.
     *
     * Выберет из таблицы истории последние актуальные рационы за последние n дней:
     *
     *  - `WHERE id in ( SELECT max(id) FROM history GROUP BY date )`
     *
     * Возьмёт для них информацию об дневном КБЖУ.
     *
     * @param login пользователя, информация о котором запрашивается
     * @param days сколько дней в промежутке (сегодня - [days])
     * @return Значения КБЖУ по датам
     */
    fun getHistoryTDEEForUser(login: String, days: Int): Map<String, HistoryRow> {
        if (days < 1) throw IllegalArgumentException("days must be greater than 0")

        val connection = Database.getPGConnection()

        // Под-запрос с GROUP BY нужен, так как может возникать несколько записей для одного дня (из-за обновления рациона).
        // Где больше id - там и правда.
        val statement = connection.prepareStatement(
            """SELECT date, total_tdee, total_protein, total_fat, total_carbs FROM nutrition.history
                | WHERE login = ? AND date >= NOW() - INTERVAL ? DAY
                |    AND id in ( SELECT max(id) FROM nutrition.history GROUP BY date )
                | ORDER BY date;""".trimMargin(),
        )
        statement.setString(1, login)
        statement.setInt(2, days - 1)

        val result = statement.executeQuery().use { rs ->
            buildMap {
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

    /**
     * Получить последний актуальный рацион на сегодня
     *
     * В таблице хранится вся история, включая повторные генерации. Больший id - более актуальная запись рациона.
     *
     * Выберет из таблицы для этого пользователя максимальный id по сегодняшней дате:
     *
     *  - `WHERE login = ? AND date = now() ORDER BY id DESC LIMIT 1`
     *
     * @param login пользователя, информация о котором запрашивается
     * @return Полную строку истории
     */
    fun getTodayHistoryForUser(login: String): HistoryFullDTO {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement(
            "SELECT * FROM nutrition.history WHERE login = ? AND date = now() ORDER BY id DESC LIMIT 1"
        )
        statement.setString(1, login)

        val result = statement.executeQuery().use { rs ->
            {
                if (rs.next()) {
                    HistoryFullDTO(
                        rs.getString("login"),
                        rs.getObject("date", LocalDate::class.java),
                        rs.getLong("breakfast"),
                        rs.getInt("breakfast_weight"),
                        rs.getLong("lunch"),
                        rs.getInt("lunch_weight"),
                        rs.getLong("dinner"),
                        rs.getInt("dinner_weight"),
                        rs.getDouble("total_tdee"),
                        rs.getDouble("total_protein"),
                        rs.getDouble("total_fat"),
                        rs.getDouble("total_carbs"),
                    )
                } else {
                    throw Exception("No history found for today")
                }
            }
        }()

        statement.close()
        connection.close()
        return result
    }

    /**
     * Записать дневной рацион в историю
     *
     * @param login для какого пользователя составлен рацион
     * @param dishSet сам рацион
     */
    fun addHistory(login: String, dishSet: DailyDishSetDTO) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement(
            """INSERT INTO nutrition.history VALUES
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

    /**
     * Получает последний актуальный рацион за указанную дату.
     *
     * @return рацион в представлении, используемом при генерации. ([DailyDishSetDTO])
     */
    fun getFromHistoryRationByDate(login: String, date: LocalDate): DailyDishSetDTO {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement(
            "SELECT * FROM nutrition.history WHERE login = ? AND date = ? ORDER BY id DESC LIMIT 1"
        )
        statement.setString(1, login)
        statement.setObject(2, date)

        val result = statement.executeQuery().use { rs ->
            {
                if (rs.next()) {
                    DailyDishSetDTO(
                        DishService.getDishById(rs.getLong("breakfast"))
                            .adjustWeight(rs.getInt("breakfast_weight").toDouble()),
                        DishService.getDishById(rs.getLong("lunch"))
                            .adjustWeight(rs.getInt("lunch_weight").toDouble()),
                        DishService.getDishById(rs.getLong("dinner"))
                            .adjustWeight(rs.getInt("dinner_weight").toDouble()),
                        rs.getDouble("total_tdee"),
                        rs.getDouble("total_protein"),
                        rs.getDouble("total_fat"),
                        rs.getDouble("total_carbs"),
                    )
                } else {
                    throw Exception("No history found for this user or date")
                }
            }
        }()

        statement.close()
        connection.close()
        return result
    }
}