package controller

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ErrorType
import model.request.LoginDto
import model.request.RegisterDto
import model.request.RequestWrapper
import model.response.*
import service.UserService
import validation.AuthValidator
import validation.UserDataValidator
import java.util.*

class AuthController(private val userService: UserService) {
    fun handleRegister(requestBody: String) {
        println("Register request: $requestBody")
        val registerRequest: RequestWrapper<RegisterDto> = try {
            Json.decodeFromString(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendError(UUID.fromString("-1"), error)
            return
        }
        val registerDto = registerRequest.dto
        try {
            if (AuthValidator.authValidation(registerDto.username, registerDto.password) &&
                UserDataValidator.userDataValidation(
                    registerDto.name, registerDto.heightCm,
                    registerDto.weightKg, registerDto.birthDate
                )
            ) {

                val token = userService.register(registerDto.username, registerDto.password)
                if (token != null) {
                    val response = LoginResponse(jwt = token)
                    sendResponse("auth:response:Register", registerRequest.uuid, response)
                    val userDataRequest = UserDataDto(
                        username = registerDto.username,
                        name = registerDto.name,
                        birthDate = registerDto.birthDate,
                        gender = registerDto.gender,
                        heightCm = registerDto.heightCm,
                        weightKg = registerDto.weightKg,
                        weightDesire = registerDto.weightDesire
                    )
                    sendRequest("user_data:request:CreateUserData", userDataRequest)
                } else {
                    val errorMessage = "User with this username already exists"
                    val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                    sendError(registerRequest.uuid, error)
                }
            } else {
                val errorMessage = "Data is not valid"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendError(registerRequest.uuid, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(registerRequest.uuid, error)
        }
    }

    fun handleLogin(requestBody: String) {
        println("Login request: $requestBody")
        val loginRequest: RequestWrapper<LoginDto> = try {
            Json.decodeFromString(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendError(UUID.fromString("-1"), error)
            return
        }
        val loginDto = loginRequest.dto
        try {
            if (AuthValidator.authValidation(loginDto.username, loginDto.password)) {

                val token = userService.login(loginDto.username, loginDto.password)
                if (token != null) {
                    val response = LoginResponse(jwt = token)
                    sendResponse("auth:response:Login", loginRequest.uuid, response)

                } else {
                    val errorMessage = "User unauthorized!"
                    val error = ErrorDto(ErrorType.UNAUTHORIZED, errorMessage)
                    sendError(loginRequest.uuid, error)

                }
            } else {
                val errorMessage = "Data is not valid"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendError(loginRequest.uuid, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(loginRequest.uuid, error)
        }
    }

    private fun sendRequest(@Suppress("SameParameterValue") channel: String, dto: UserDataDto) {
        val requestJson = Json.encodeToString(dto)
        sendEvent(channel, requestJson)
    }

    private fun sendResponse(channel: String, id: UUID, dto: LoginResponse) {
        val response = ResponseWrapper(id, dto)
        val responseJson = Json.encodeToString(response)
        sendEvent(channel, responseJson)
    }

    private fun sendError(id: UUID, dto: ErrorDto) {
        val response = ErrorWrapper(id, dto)
        val responseJson = Json.encodeToString(response)
        sendEvent("error", responseJson)
    }
}