import keydb.runServiceListener
import keydb.sendEvent

fun afterStartup(){
//    sendEvent("jwtRevokeRequest", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzbGF2YSIsImlhdCI6MTc0MTk4NjYwOCwiZXhwIjoxNzQxOTk1MjQ4fQ.DUO-_ljg4lA4y0TZ6mQ2vI-1QINnj_xbA24gA5nMtgI")
//    val jsonString = """
//        {
//          "id": 1,
//          "dto": {
//            "username": "johnis_doe",
//            "password": "securepassword123",
//            "name": "John Doe",
//            "birthDate": "1990-05-15",
//            "gender": "MALE",
//            "heightCm": 180,
//            "weightKg": 75.5,
//            "weightDesire": "LOSS"
//          }
//        }
//    """.trimIndent()
//    sendEvent("registerRequest", jsonString)
//    val loginJson = """
//        {
//          "id": 1,
//          "dto": {
//            "username": "johnis_doe",
//            "password": "securepassword123"
//          }
//        }
//    """.trimIndent()
//    sendEvent("loginRequest", loginJson)
//
    sendEvent("jwtValidationRequest", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huaXNfZG9lIiwiaWF0IjoxNzQyMzMyNzg3LCJleHAiOjE3NDIzNDE0Mjd9.WS_TMyKV-WGaVN4zESKadzOcO_yRVZQ55bZLXs02Ea0")
}
fun receiveMessage(msg: String){
    println(msg)
}

fun main(): Unit = runServiceListener(
    mapOf(
        "jwtValidationResponse" to ::receiveMessage,
        "jwtRevokeResponse" to ::receiveMessage,
        "createUserDataRequest" to ::receiveMessage,
        "loginResponse" to ::receiveMessage,
    ),
    ::afterStartup
)