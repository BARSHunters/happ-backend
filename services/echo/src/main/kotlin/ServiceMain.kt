import keydb.runServiceListener
import keydb.sendEvent

fun echoHandler(message: String) = sendEvent("channel", message).also { println("got a message!") }

fun main(): Unit = runServiceListener(
    mapOf(
        "echo" to ::echoHandler,
    )
)
