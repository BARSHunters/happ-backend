package org.example.api

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.dto.*
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

    /**
     * Запрос на последний актуальный рацион за указанную дату.
     *
     * @see HistoryService.getFromHistoryRationByDate
     * @param msg Ожидается json соответсвующий [HistoryRequestRationByDateDTO]
     * @return [Unit], но отправит в KeyDB результат в формате [DailyDishSetDTO]
     */
    fun getRationByDate(msg: String) {
        val request: HistoryRequestRationByDateDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }

        sendEvent(
            "nutrition:response:ration_by_date", Json.encodeToString(
                RationResponseDTO(
                    request.id,
                    HistoryService.getFromHistoryRationByDate(request.login, request.date),
                )
            )
        )
    }
}