package org.example

import keydb.runServiceListener
import org.example.api.HistoryController
import org.example.api.RationController


/**
 * Инициация приложения
 *
 * Вызывает инициализацию подключений к БД
 */
fun afterStartup() {
    try {
        Database
    }
    catch(e: Exception) {
        System.err.println("Exception thrown while trying to connect to the database")
        System.err.println(e.message)
    }
}

/**
 * Точка входа.
 *
 * Монтирует каналы KeyDB к функциям обработчикам.
 */
fun main(): Unit = runServiceListener(
    mapOf(
        // Генерация рациона
        "nutrition:request:today_ration" to RationController::requestTodayRation, // входная точка
        "weight_history:response:WeightControlWish" to RationController::afterFetchFromWeightHistoryService, // продолжение обработки
        "activity:response:ActivityIndex" to RationController::afterFetchFromActivityService, // продолжение обработки
        "user_data:response:UserData" to RationController::afterFetchFromUserDataService, // продолжение обработки

        // Обновление рациона по 1 блюду
        "nutrition:request:update_today_ration" to RationController::updateTodayRation,

        // История рационов
        "nutrition:request:CPFC" to HistoryController::getRationHistory,
        "nutrition:request:ration_by_date" to HistoryController::getRationByDate,
    ),
    ::afterStartup
)