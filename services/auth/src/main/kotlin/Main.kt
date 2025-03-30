import controller.AuthController
import database.Database
import keydb.runServiceListener
import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ErrorType
import model.request.TokenDto
import model.response.ErrorDto
import model.response.MessageResponse
import model.response.TokenValidationResponse
import repository.TokenRepository
import repository.UserRepository
import service.UserService

lateinit var userService: UserService
lateinit var authController: AuthController
fun afterStartup() {
    Database
    val userRepository = UserRepository()
    val tokenRepository = TokenRepository()
    println("Init User Service")
    userService = UserService(userRepository, tokenRepository)
    println("Init Auth Controller")
    authController = AuthController(userService)
    println("Service auth is running")

}

fun receiveJwtToken(requestBody: String) {
    println("Receive Jwt Token $requestBody")
    val request: TokenDto = try {
        Json.decodeFromString<TokenDto>(requestBody)
    } catch (e: Exception) {
        e.printStackTrace()
        val errorMessage = "Invalid JSON format"
        val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
        val response = Json.encodeToString(error)
        sendEvent("error", response)
        return
    }
    val token = request.token
    val username = userService.validateJwtToken(token)
    if (username != null) {
        val response = TokenValidationResponse(request.id, "valid", username)
        sendEvent("auth:response:JwtValidation", Json.encodeToString(response))
    } else {
        val response = TokenValidationResponse(request.id, "invalid", "")
        println("response = $response")
        sendEvent("auth:response:JwtValidation", Json.encodeToString(response))
    }
}

fun revokeJwtToken(requestBody: String) {
    println("Revoke Jwt Token $requestBody")
    val request: TokenDto = try {
        Json.decodeFromString<TokenDto>(requestBody)
    } catch (e: Exception) {
        e.printStackTrace()
        val errorMessage = "Invalid JSON format"
        val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
        val response = Json.encodeToString(error)
        sendEvent("error", response)
        return
    }
    val token = request.token
    if (userService.revokeJwtToken(token)) {
        val response = MessageResponse(request.id, "success")
        sendEvent("auth:response:JwtRevoke", Json.encodeToString(response))
    } else {
        val response = MessageResponse(request.id, "error")
        sendEvent("auth:response:JwtRevoke", Json.encodeToString(response))
    }
}

fun loginRequest(loginRequest: String) {
    authController.handleLogin(loginRequest)
}

fun registerRequest(registerRequest: String) {
    authController.handleRegister(registerRequest)
}

fun main(): Unit = runServiceListener(
    mapOf(
        "auth:request:Register" to ::registerRequest,
        "auth:request:Login" to ::loginRequest,
        "auth:request:JwtValidation" to ::receiveJwtToken,
        "auth:request:JwtRevoke" to ::revokeJwtToken
    ),
    ::afterStartup
)