package org.example.service

import org.example.DB
import org.example.Database
import org.example.decider.Wish
import org.example.dto.MealType
import org.example.dto.RationCacheDTO
import org.example.dto.RationRequestDTO
import org.example.dto.UpdateRationRequestDTO
import java.util.*

/**
 * Сервис, работающий с таблицей кеширования запросов пользователей в Postgres
 */
object RationCacheService {

    init {
        Database.initService("V0_0_2__INIT_PG_CACHE.sql", DB.PG)
    }

    /**
     * Первая запись кеша
     *
     * Содержит только UUID запроса и login пользователя к которому он относится.
     */
    fun initQuery(request: RationRequestDTO) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("INSERT INTO nutrition.cache_ration (query_id, login) VALUES (?,?)")
        statement.setObject(1, request.id)
        statement.setString(2, request.login)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }

    /**
     * Первая запись кеша для запроса на обновление конкретного приёма пищи
     *
     * Содержит только UUID запроса, login пользователя к которому он относится, и прим пищи.
     */
    fun initUpdateQuery(request: UpdateRationRequestDTO) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("INSERT INTO nutrition.cache_ration (query_id, login, meal_type) VALUES (?,?,?)")
        statement.setObject(1, request.id)
        statement.setString(2, request.login)
        statement.setString(3, request.type.name)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }

    /**
     * Запомнить пожелание о весе
     *
     * @param queryId UUID запроса кеш которого надо обновить
     * @param wish пожелание, которое надо добавить
     */
    fun saveWish(queryId: UUID, wish: Wish) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("UPDATE nutrition.cache_ration SET wish = ? WHERE query_id = ?")
        statement.setString(1, wish.name)
        statement.setObject(2, queryId)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }

    /**
     * Запомнить индекс активности пользователя
     *
     * @param queryId UUID запроса кеш которого надо обновить
     * @param activityIndex индекс, который надо добавить
     */
    fun saveActivity(queryId: UUID, activityIndex: Float) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("UPDATE nutrition.cache_ration SET activity_index = ? WHERE query_id = ?")
        statement.setFloat(1, activityIndex)
        statement.setObject(2, queryId)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }


    /**
     * Получить всю строку кеша
     *
     * @param queryId UUID запроса кеш которого надо получить
     */
    fun getByQueryId(queryId: UUID): RationCacheDTO {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("SELECT * FROM nutrition.cache_ration WHERE query_id = ?")
        statement.setObject(1, queryId)

        val resultSet = statement.executeQuery()
        return if (resultSet.next()) {
            RationCacheDTO(
                resultSet.getObject("query_id", UUID::class.java),
                resultSet.getString("login"),
                try { Wish.valueOf(resultSet.getString("wish")) } catch (e: Exception) { null },
                try { MealType.valueOf(resultSet.getString("meal_type")) } catch (e: Exception) { null },
                try { resultSet.getFloat("activity_index") } catch (e: Exception) { null },
            )
        } else {
            throw Exception("No rows with query_id=$queryId")
        }.also {
            resultSet.close()
            statement.close()
            connection.close()
        }
    }


    /**
     * Отчистить кеш
     *
     * @param queryId UUID запроса кеш которого надо стереть
     */
    fun clearQuery(queryId: UUID) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("DELETE FROM nutrition.cache_ration WHERE query_id = ?")
        statement.setObject(1, queryId)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }
}