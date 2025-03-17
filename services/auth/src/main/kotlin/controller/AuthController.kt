package controller

import com.sun.net.httpserver.HttpExchange
import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import loginRequest
import model.ErrorType
import model.request.LoginDto
import model.request.RegisterDto
import model.request.RequestWrapper
import model.response.ErrorDto
import model.response.ErrorWrapper
import model.response.LoginResponse
import model.response.ResponseWrapper
import registerRequest
import service.UserService

class AuthController(private val userService: UserService) {

    // TODO добавить логику ответа для сервисов и фронта
    fun handleRegister(requestBody: String){
        println("Register request: $requestBody")
        val registerRequest: RequestWrapper<RegisterDto> = Json.decodeFromString(requestBody)
        val registerDto = registerRequest.dto
        try {
            val token = userService.register(registerDto.username, registerDto.password)
            if (token != null) {
                val response = LoginResponse(jwt = token)
                sendResponse("registerResponse", registerRequest.id, response)
            } else {
                val errorMessage = "User with this username already exists"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendError(registerRequest.id, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(registerRequest.id, error)
        }
    }

    // TODO добавить логику для end-point login
    fun handleLogin(requestBody: String){
        println("Login request: $requestBody")
        val loginRequest: RequestWrapper<LoginDto> = Json.decodeFromString(requestBody)
        val loginDto = loginRequest.dto
        try {
            val token = userService.login(loginDto.username, loginDto.password)
            if (token != null) {
                val response = LoginResponse(jwt = token)
                sendResponse("loginResponse", loginRequest.id, response)

            } else {
                val errorMessage = "User unauthorized!"
                val error = ErrorDto(ErrorType.UNAUTHORIZED, errorMessage)
                sendError(loginRequest.id, error)

            }
        }catch (e: Exception){
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(loginRequest.id, error)
        }
    }

    private fun sendResponse(channel: String, id: Int, dto: Any){
        val response = ResponseWrapper(id, dto)
        val responseJson = Json.encodeToString(response)
        sendEvent(channel, responseJson)
    }
    private fun sendError(id: Int, dto: ErrorDto){
        val response = ErrorWrapper(id, dto)
        val responseJson = Json.encodeToString(response)
        sendEvent("error", responseJson)
    }
}