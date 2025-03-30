package org.example.api

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.decider.Decider
import org.example.decider.Wish
import org.example.dto.*
import org.example.model.User
import org.example.service.HistoryService
import org.example.service.RationCacheService

/**
 * API для работы с генерацией рационов питания.
 */
object RationController {
    /**
     * Старт генерации рациона.
     *
     * Сохранит в кэше (см. [RationCacheService]) UUID запроса и login пользователя.
     *
     * После получения запроса обработает входной json и пойдет за нужной информацией в другие сервисы.
     * Процесс исполнения (генерации рациона) продолжится в
     * [afterFetchFromWeightHistoryService], [afterFetchFromActivityService] и [afterFetchFromUserDataService]
     *
     * @param msg Ожидается json соответсвующий [RationRequestDTO]
     * @return [Unit], но отправит запрос на получение [пожеланий пользователя][Wish] насчёт веса.
     */
    fun requestTodayRation(msg: String) {
        val request: RationRequestDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }
        RationCacheService.initQuery(request)
        sendEvent("weight_history:request:WeightControlWish", Json.encodeToString(request))
    }

    /**
     * Промежуточный этап генерации рациона.
     *
     * Запишет полученное [пожелание пользователя о весе][Wish] в кэше по тому же UUID запроса (см. [RationCacheService]).
     *
     * Процесс исполнения (генерации рациона) продолжится в
     * [afterFetchFromActivityService] и [afterFetchFromUserDataService]
     *
     * @param msg Ожидается json соответсвующий [WishResponseDTO]
     * @return [Unit], но отправит запрос на получение [данных пользователя][UserDTO].
     */
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
            cache = RationCacheService.getByQueryId(request.id)
        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent("error", Json.encodeToString(ErrorDTO(request.id, "Skipped stages for this query")))
            return
        }

        RationCacheService.saveWish(request.id, request.wish)

        sendEvent("activity:request:ActivityIndex", Json.encodeToString(RationRequestDTO(request.id, cache.login)))
    }

    /**
     * Промежуточный этап генерации рациона.
     *
     * Запишет полученный индекс активности в кэше по тому же UUID запроса (см. [RationCacheService]).
     *
     * Процесс исполнения (генерации рациона) продолжится в [afterFetchFromUserDataService]
     *
     * @param msg Ожидается json соответсвующий [WishResponseDTO]
     * @return [Unit], но отправит запрос на получение [данных пользователя][UserDTO].
     */
    fun afterFetchFromActivityService(msg: String) {
        val request: ActivityResponseDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }

        val cache: RationCacheDTO
        try {
            cache = RationCacheService.getByQueryId(request.id)
        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent("error", Json.encodeToString(ErrorDTO(request.id, "Skipped stages for this query")))
            return
        }

        RationCacheService.saveActivity(request.id, request.activityIndex)

        sendEvent("user_data:request:UserData", Json.encodeToString(UserDataRequestDTO(request.id, cache.login)))
    }

    /**
     * Финальный этап генерации рациона.
     *
     * По UUID запроса получит из кеша (см. [RationCacheService]):
     *  - login пользователя
     *  - [Его пожелание о весе][Wish].
     *  - Его индекс активности
     *  - Если это обновление [по одному приему пищи][MealType], то что это за приём пищи. (см. [updateTodayRation])
     *
     * Для генерации вызовет [Decider.decide] (или [Decider.swap] если это обновление по одному приему пищи - см. [updateTodayRation])
     *
     * @param msg Ожидается json соответсвующий [UserDTO]
     * @return [Unit], но отправит в KeyDB новый рацион ([RationResponseDTO]).
     */
    fun afterFetchFromUserDataService(msg: String) {
        val request: UserDataResponseDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }

        val cache: RationCacheDTO
        try {
            cache = RationCacheService.getByQueryId(request.id)
        } catch (e: Exception) {
            e.printStackTrace()
            sendEvent("error", Json.encodeToString(ErrorDTO(request.id, "Skipped stages for this query")))
            return
        }

        val user = User(request.dto, cache.activityIndex ?: throw NullPointerException("activity index is null"))

        val dishSet: DailyDishSetDTO = if (cache.type != null) {
            Decider.swap(user, cache.wish ?: Wish.KEEP, cache.type).getOrElse {
                sendEvent("error", Json.encodeToString(ErrorDTO(request.id, it.message ?: "Couldn't generate ration")))
                return
            }
        } else {
            Decider.decide(user, cache.wish ?: Wish.KEEP).getOrElse {
                sendEvent("error", Json.encodeToString(ErrorDTO(request.id, it.message ?: "Couldn't generate ration")))
                return
            }
        }

        RationCacheService.clearQuery(request.id)
        HistoryService.addHistory(cache.login, dishSet)
        sendEvent("nutrition:response:ration", Json.encodeToString(RationResponseDTO(request.id, dishSet)))
    }

    /**
     * Старт обновления рациона по одному приёму пищи.
     *
     * Сохранит в кэше (см. [RationCacheService]) UUID запроса и login пользователя.
     *
     * После получения запроса обработает входной json и пойдет за нужной информацией в другие сервисы.
     * Процесс исполнения (генерации рациона) продолжится в
     * [afterFetchFromWeightHistoryService], [afterFetchFromActivityService] и [afterFetchFromUserDataService]
     *
     * @param msg Ожидается json соответсвующий [UpdateRationRequestDTO]
     * @return [Unit], но отправит запрос на получение [пожеланий пользователя][Wish] насчёт веса.
     */
    fun updateTodayRation(msg: String) {
        val request: UpdateRationRequestDTO = try {
            Json.decodeFromString(msg)
        } catch (e: SerializationException) {
            e.printStackTrace()
            sendEvent("error", "Invalid JSON format")
            return
        }
        RationCacheService.initUpdateQuery(request)
        sendEvent(
            "weight_history:request:WeightControlWish",
            Json.encodeToString(RationRequestDTO(request.id, request.login))
        )
    }
}