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
        val statement = connection.prepareStatement("INSERT INTO cache_ration (query_id, login) VALUES (?,?)")
        statement.setObject(1, request.queryId)
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
        val statement = connection.prepareStatement("INSERT INTO cache_ration (query_id, login, meal_type) VALUES (?,?,?)")
        statement.setObject(1, request.queryId)
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
        val statement = connection.prepareStatement("UPDATE cache_ration SET wish = ? WHERE query_id = ?")
        statement.setString(1, wish.name)
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
        val statement = connection.prepareStatement("SELECT * FROM cache_ration WHERE query_id = ?")
        statement.setString(1, queryId.toString())

        val result = statement.executeQuery().use { rs ->
            {
                if (rs.next()) {
                    RationCacheDTO(
                        rs.getObject("query_id", UUID::class.java),
                        rs.getString("login"),
                        rs.getObject("wish", Wish::class.java),
                        rs.getObject("meal_type", MealType::class.java),
                    )
                } else {
                    throw Exception("No rows with query_id=$queryId")
                }
            }
        }()

        statement.close()
        connection.close()
        return result
    }


    /**
     * Отчистить кеш
     *
     * @param queryId UUID запроса кеш которого надо стереть
     */
    fun clearQuery(queryId: UUID) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("DELETE FROM cache_ration WHERE query_id = ?")
        statement.setObject(1, queryId)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }
}