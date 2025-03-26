package org.example.service

import org.example.Database
import org.example.dto.RationCacheDTO
import org.example.dto.RationRequestDTO
import java.util.*

object RationCacheService {
    fun initQuery(request: RationRequestDTO) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("INSERT INTO cache_ration (query_id, login) VALUES (?,?)")
        statement.setObject(1, request.queryId)
        statement.setString(2, request.login)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }

    fun saveWish(queryId: UUID, wish: String) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("UPDATE cache_ration SET wish = ? WHERE query_id = ?")
        statement.setString(1, wish)
        statement.setObject(2, queryId)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }

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
                        rs.getString("wish"),
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

    fun clearQuery(queryId: UUID) {
        val connection = Database.getPGConnection()
        val statement = connection.prepareStatement("DELETE FROM cache_ration WHERE query_id = ?")
        statement.setObject(1, queryId)
        statement.executeUpdate()
        statement.close()
        connection.close()
    }
}