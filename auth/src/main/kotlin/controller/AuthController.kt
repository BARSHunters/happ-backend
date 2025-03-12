package controller

import com.sun.net.httpserver.HttpExchange
import kotlinx.serialization.json.Json
import model.request.RegisterDto
import service.UserService

class AuthController(private val userService: UserService) {

    // TODO добавить логику ответа для сервисов и фронта
    fun handleRegister(exchange: HttpExchange){
        val requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
        val registerRequest = Json.decodeFromString<RegisterDto>(requestBody)

        val success = userService.register(registerRequest.username, registerRequest.password)

    }

    // TODO добавить логику для end-point login
    fun handleLogin(exchange: HttpExchange){

    }
}