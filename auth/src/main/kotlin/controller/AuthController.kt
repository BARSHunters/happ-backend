package controller

import com.sun.net.httpserver.HttpExchange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.request.LoginDto
import model.request.RegisterDto
import model.response.LoginResponse
import service.UserService

class AuthController(private val userService: UserService) {

    // TODO добавить логику ответа для сервисов и фронта
    fun handleRegister(exchange: HttpExchange){
        val requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
        println("Register request: $requestBody")
        val registerRequest = Json.decodeFromString<RegisterDto>(requestBody)

        val token = userService.register(registerRequest.username, registerRequest.password)
        if (token != null){
            val response = LoginResponse(jwt = token)
            val jsonResponse = Json.encodeToString(response)
            sendResponse(exchange, jsonResponse, 200)
        }
        else {
            val errorResponse = """{"error": "Bad request"}"""
            sendResponse(exchange, errorResponse, 400)
        }
    }

    // TODO добавить логику для end-point login
    fun handleLogin(exchange: HttpExchange){
        try {


            val requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
            println("Login request: $requestBody")
            val registerRequest = Json.decodeFromString<LoginDto>(requestBody)

            val token = userService.login(registerRequest.username, registerRequest.password)
            if (token != null) {
                println("jwt: $token")
                val response = LoginResponse(jwt = token)
                val jsonResponse = Json.encodeToString(response)
                sendResponse(exchange, jsonResponse, 200)

            } else {
                val errorResponse = """{"error": "User unauthorized"}"""
                sendResponse(exchange, errorResponse, 401)
            }
        }catch (e: Exception){
            e.printStackTrace()
            sendResponse(exchange, "Internal Server Error", 500)
        }
    }

    private fun sendResponse(exchange: HttpExchange, responseMessage: String, rCode: Int){
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(rCode, responseMessage.toByteArray().size.toLong())
        exchange.responseBody.use { it.write(responseMessage.toByteArray()) }
    }
}