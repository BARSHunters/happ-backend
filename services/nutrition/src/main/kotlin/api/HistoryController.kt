package org.example.api

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.dto.HistoryRequestDTO
import org.example.dto.HistoryResponseDTO
import org.example.service.HistoryService

object HistoryController {

    fun getRationHistory(msg: String) {
        val request: HistoryRequestDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }

        sendEvent(
            "response_nutrition_data", Json.encodeToString(
                HistoryResponseDTO(
                    request.queryId,
                    HistoryService.getHistoryForUser(request.login, request.days),
                )
            )
        )

    }
}