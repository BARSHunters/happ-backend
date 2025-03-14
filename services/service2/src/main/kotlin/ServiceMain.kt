import keydb.runServiceListener
import keydb.sendEvent

fun receiveMessage(msg: String) = println(msg)

fun afterStartup() {
    sendEvent("echo", "hello!")
}

fun main(): Unit = runServiceListener(
    mapOf(
        "channel" to ::receiveMessage,
    ),

    ::afterStartup
)
