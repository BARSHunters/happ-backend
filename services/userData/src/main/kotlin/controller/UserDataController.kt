package controller

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ErrorType
import model.UserData
import model.request.GetterDto
import model.request.RequestWrapper
import model.response.ErrorDto
import model.response.ResponseWrapper
import model.response.WeightHistoryResponse
import service.UserDataService
import validation.UserDataValidator
import java.util.*

class UserDataController(private val userDataService: UserDataService) {
    fun handleCreateUserData(requestBody: String) {
        println(requestBody)
        val request: RequestWrapper<UserData> = try {
            Json.decodeFromString(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val userData = request.dto
        try {
            if (UserDataValidator.userDataValidation(userData)) {
                val result = userDataService.createUserData(userData)
                if (result) {
                    val weightHistoryResponse = WeightHistoryResponse(
                        username = userData.username,
                        weightKg = userData.weightKg,
                    )
                    sendResponse("weight_history:request:NewWeight", request.uuid, weightHistoryResponse)
                } else {
                    val errorMessage = "Can't create user data"
                    val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                    sendResponse("error", request.uuid, error)
                }
            } else {
                val errorMessage = "Data is invalid"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendResponse("error", request.uuid, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal server error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun handleUpdateUserData(requestBody: String) {
        println(requestBody)
        val request: RequestWrapper<UserData> = try {
            Json.decodeFromString(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val userData = request.dto
        try {
            if (UserDataValidator.userDataValidation(userData)) {
                val result = userDataService.updateUserData(userData)
                if (result) {
                    val weightHistoryResponse = WeightHistoryResponse(
                        username = userData.username,
                        weightKg = userData.weightKg,
                    )
                    sendResponse("weight_history:request:NewWeight", request.uuid, weightHistoryResponse)
                } else {
                    val errorMessage = "Can't update user data"
                    val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                    sendResponse("error", request.uuid, error)
                }
            } else {
                val errorMessage = "Data is invalid"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendResponse("error", request.uuid, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal server error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun receiveUserData(requestBody: String) {
        println("Get user data request $requestBody")
        val request: GetterDto = try {
            Json.decodeFromString<GetterDto>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val username = request.username
        val userData = userDataService.getUserData(username)
        if (userData != null) {
            sendResponse("user_data:response:UserData", request.uuid, userData)
        } else {
            val errorMessage = "Can't get user data for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun receiveName(requestBody: String) {
        val request: GetterDto = try {
            Json.decodeFromString<GetterDto>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val username = request.username
        val userName = userDataService.getName(username)
        if (userName != null) {
            sendResponse("user_data:response:Name", request.uuid, userName)
        } else {
            val errorMessage = "Can't get name for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun receiveGender(requestBody: String) {
        val request: GetterDto = try {
            Json.decodeFromString<GetterDto>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val username = request.username
        val userGender = userDataService.getGender(username)
        if (userGender != null) {
            sendResponse("user_data:response:Gender", request.uuid, userGender.name)
        } else {
            val errorMessage = "Can't get gender for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun receiveBirthDate(requestBody: String) {
        val request: GetterDto = try {
            Json.decodeFromString<GetterDto>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val username = request.username
        val userBirthDate = userDataService.getBirthDate(username)
        if (userBirthDate != null) {
            sendResponse("user_data:response:BirthDate", request.uuid, userBirthDate.toString())
        } else {
            val errorMessage = "Can't get birthDate for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun receiveAge(requestBody: String) {
        val request: GetterDto = try {
            Json.decodeFromString<GetterDto>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val username = request.username
        val userAge = userDataService.getAge(username)
        if (userAge != null) {
            sendResponse("user_data:response:Age", request.uuid, userAge.toString())
        } else {
            val errorMessage = "Can't get age for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun receiveHeight(requestBody: String) {
        val request: GetterDto = try {
            Json.decodeFromString<GetterDto>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val username = request.username
        val userHeight = userDataService.getHeightCm(username)
        if (userHeight != null) {
            sendResponse("user_data:response:Height", request.uuid, userHeight.toString())
        } else {
            val errorMessage = "Can't get height for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun receiveWeight(requestBody: String) {
        val request: GetterDto = try {
            Json.decodeFromString<GetterDto>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val username = request.username
        val userWeight = userDataService.getWeightKg(username)
        if (userWeight != null) {
            sendResponse("user_data:response:Weight", request.uuid, userWeight.toString())
        } else {
            val errorMessage = "Can't get weight for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    fun receiveWeightDesire(requestBody: String) {
        val request: GetterDto = try {
            Json.decodeFromString<GetterDto>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", UUID.fromString("-1"), error)
            return
        }
        val username = request.username
        val userWeightDesire = userDataService.getWeightDesire(username)
        if (userWeightDesire != null) {
            sendResponse("user_data:response:WeightDesire", request.uuid, userWeightDesire.name)
        } else {
            val errorMessage = "Can't get weight-desire for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", request.uuid, error)
        }
    }

    private inline fun <reified T> sendResponse(channel: String, id: UUID, response: T) {
        val responseWrapper = ResponseWrapper(id, response)
        val responseJson = Json.encodeToString(responseWrapper)
        sendEvent(channel, responseJson)
    }
}