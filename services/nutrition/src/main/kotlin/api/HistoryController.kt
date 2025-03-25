package org.example.api

import keydb.sendEvent

object HistoryController {

    fun getRationHistory(msg: String) {
        // TODO спросить в бд

        sendEvent("response_nutrition_data", "hello!") // TODO залить в kdb
    }
}