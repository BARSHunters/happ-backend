package org.example

import keydb.runServiceListener
import keydb.sendEvent
import org.example.api.HistoryController
import org.example.api.RationController

fun receiveMessage(msg: String) = println(msg)

fun afterStartup() {
    try {
        Database
    }
    catch(e: Exception) {
        System.err.println("Exception thrown while trying to connect to the database")
        System.err.println(e.message)
    }
    sendEvent("hello_placeholder", "Hello world!").also { println("got a message") }
}

// TODO Dokka
fun main(): Unit = runServiceListener(
    mapOf(
        "hello_placeholder" to ::receiveMessage,

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