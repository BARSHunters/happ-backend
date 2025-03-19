import database.Database
import keydb.runServiceListener
import keydb.sendEvent

fun afterStartup(){
    Database
}
fun receiveMessage(msg: String){
    println(msg)
}

fun main(): Unit = runServiceListener(
    mapOf(
        "jwtValidationResponse" to ::receiveMessage,
        "createUserDataRequest" to ::receiveMessage,
    ),
    ::afterStartup
)