import database.Database
import keydb.runServiceListener

fun afterStartup(){
    Database
    println("Service userData is running")
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