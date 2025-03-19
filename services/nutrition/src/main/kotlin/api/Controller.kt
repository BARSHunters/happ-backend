package org.example.api

import keydb.sendEvent

object Controller {
    fun requestTodayRation(msg: String) {
        sendEvent("helloPlaceholder", "hello!") // TODO получит тип рациона: для набора/сброса/поддержания массы

        sendEvent("helloPlaceholder", "hello!") // TODO получить данные пользователя (возраст, рост, текущий вес, пол,
        // индекс активности, предпочитаемые формулы расчета и доп данные)

        // TODO калькуляторы

        // TODO решение о рационе

        // TODO запись рациона

        sendEvent("helloPlaceholder", "hello!") // TODO set ready status
    }

    fun getRationHistory(msg: String) {
        // TODO спросить в бд

        sendEvent("helloPlaceholder", "hello!") // TODO залить в kdb
    }
}