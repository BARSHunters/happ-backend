import controller.AuthController
import database.Database
import keydb.runServiceListener
import keydb.sendEvent
import repository.TokenRepository
import repository.UserRepository
import service.UserService

lateinit var userService: UserService
lateinit var authController: AuthController
fun afterStartup(){
    Database
    val userRepository = UserRepository()
    val tokenRepository = TokenRepository()
    userService = UserService(userRepository, tokenRepository)
    authController = AuthController(userService)
    println("Service auth is running")

}
fun receiveJwtToken(token: String){
    if(userService.validateJwtToken(token)){
        sendEvent("jwtValidationResponse", "valid")
    } else {
        sendEvent("jwtValidationResponse", "invalid")
    }
}
fun revokeJwtToken(token: String){
    if (userService.revokeJwtToken(token)){
        sendEvent("jwtRevokeResponse", "success")
    } else{
        sendEvent("jwtRevokeResponse", "error")
    }
}
fun loginRequest(loginRequest: String){
    authController.handleLogin(loginRequest)
}
fun registerRequest(registerRequest: String){
    authController.handleRegister(registerRequest)
}
fun main(): Unit = runServiceListener(
    mapOf(
        "registerRequest" to ::registerRequest,
        "loginRequest" to ::loginRequest,
        "jwtValidationRequest" to ::receiveJwtToken,
        "jwtRevokeRequest" to ::revokeJwtToken
    ),
    ::afterStartup
)