package org.example.api

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.decider.Decider
import org.example.dto.*
import org.example.model.User
import org.example.service.HistoryService
import org.example.service.RationCacheService

object RationController {
    fun requestTodayRation(msg: String) {
        val request: RationRequestDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }
        RationCacheService.initQuery(request)
        sendEvent("request_nutrition_wish", Json.encodeToString(request))
    }

    fun afterFetchFromWeightHistoryService(msg: String) {
        val request: WishResponseDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }

        val cache: RationCacheDTO
        try {
            cache = RationCacheService.getByQueryId(request.queryId)
        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent("error", Json.encodeToString(ErrorDTO(request.queryId, "Skipped stages for this query")))
            return
        }

        RationCacheService.saveWish(request.queryId, request.wish)

        sendEvent("getUserData", Json.encodeToString(UserDataRequestDTO(request.queryId, cache.login)))
    }

    fun afterFetchFromUserDataService(msg: String) {
        val request: UserDTO = try {
            Json.decodeFromString(msg) // FIXME форматы пока не совпадают
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }

        val user = User(request)

        val cache: RationCacheDTO
        try {
            cache = RationCacheService.getByQueryId(request.queryId)
        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent("error", Json.encodeToString(ErrorDTO(request.queryId, "Skipped stages for this query")))
            return
        }

        val dishSet = Decider.decide(user, cache.wish ?: "")

        RationCacheService.clearQuery(request.queryId)
        HistoryService.addHistory(cache.login, dishSet)
        sendEvent("nutrition:response_today_ration", Json.encodeToString(RationResponseDTO(request.queryId, dishSet)))
    }


    fun updateTodayRation(msg: String) {

    }
}