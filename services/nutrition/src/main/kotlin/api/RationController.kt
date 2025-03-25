package org.example.api

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.decider.Decider
import org.example.dto.UserDTO
import org.example.model.User

object RationController {
    fun requestTodayRation(username: String) {
        // update тут же или..? если тут же, то не просто username, а что делаем и с каким блюдом
        // TODO подумать как отправить оба (и главное обработать) sendEvent разом ради асинхронности
        sendEvent("request_nutrition_wish", username)
    }

    fun afterFetchFromWeightHistoryService(msg: String){
        // в msg должен лежать username и пожелание. username прокинуть дальше, а пожелание:
        // TODO запомнить полученный тип рациона  (для набора/сброса/поддержания массы)
        sendEvent("getUserData", "username")
    }

    fun afterFetchFromUserDataService(msg: String){
        val request: UserDTO = try {
            Json.decodeFromString(msg) // TODO форматы пока не совпадают
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }

        val user = User(request)

        val result = Decider.decide(user)

        // TODO запись рациона

        sendEvent("nutrition:response_today_ration", Json.encodeToString(result))
    }


    fun updateTodayRation(msg: String) {

    }
}