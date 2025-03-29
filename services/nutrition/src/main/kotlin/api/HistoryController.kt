package org.example.api

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.dto.HistoryRequestDTO
import org.example.dto.HistoryResponseDTO
import org.example.service.HistoryService

/**
 * API для работы с историей рационов питания.
 */
object HistoryController {

    /**
     * Запрос на получение истории КБЖУ рационов питания.
     *
     * @see HistoryService.getHistoryTDEEForUser
     * @param msg Ожидается json соответсвующий [HistoryRequestDTO]
     * @return [Unit], но отправит в KeyDB результат ([HistoryResponseDTO]).
     */
    fun getRationHistory(msg: String) {
        val request: HistoryRequestDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }

        sendEvent(
            "nutrition:response:CPFC", Json.encodeToString(
                HistoryResponseDTO(
                    request.id,
                    HistoryService.getHistoryTDEEForUser(request.login, request.days),
                )
            )
        )

    }
}