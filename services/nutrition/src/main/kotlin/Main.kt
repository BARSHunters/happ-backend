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
        "nutrition:request_today_ration" to RationController::requestTodayRation, // входная точка
        "response_nutrition_wish" to RationController::afterFetchFromWeightHistoryService, // продолжение обработки
        "sendUserData" to RationController::afterFetchFromUserDataService, // продолжение обработки

        // Обновление рациона по 1 блюду
        "nutrition:update_today_ration" to RationController::updateTodayRation,

        // История рационов
        "request_nutrition_data" to HistoryController::getRationHistory,
    ),
    ::afterStartup
)