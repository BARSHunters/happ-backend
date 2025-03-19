package org.example

import keydb.runServiceListener
import keydb.sendEvent
import org.example.api.Controller

fun receiveMessage(msg: String) = println(msg)

fun afterStartup() {
    try {
        val connection = Database.getCHConnection()
        connection.close()
        val pgConnection = Database.getPGConnection()
        pgConnection.close()
    }
    catch(e: Exception) {
        System.err.println("Exception thrown while trying to connect to the database")
        System.err.println(e.message)
    }
    sendEvent("helloPlaceholder", "Hello world!").also { println("got a message") }
}

fun main(): Unit = runServiceListener(
    mapOf(
        "helloPlaceholder" to ::receiveMessage,
        "requestTodayRation" to Controller::requestTodayRation,
        "getRationHistory" to Controller::getRationHistory,
    ),
    ::afterStartup
)