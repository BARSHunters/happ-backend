package controller

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ErrorType
import model.UserData
import model.response.ErrorDto
import model.response.WeightHistoryResponse
import service.UserDataService
import validation.UserDataValidator

class UserDataController(private val userDataService: UserDataService) {
    fun handleCreateUserData(requestBody: String) {
        println(requestBody)
        val userData: UserData = try {
            Json.decodeFromString<UserData>(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
            return
        }
        try {
            if (UserDataValidator.userDataValidation(userData)) {
                val result = userDataService.createUserData(userData)
                if (result) {
                    val weightHistoryResponse = WeightHistoryResponse(
                        username = userData.username,
                        weightKg = userData.weightKg,
                    )
                    sendResponse("request_new_weight", weightHistoryResponse)
                } else {
                    val errorMessage = "Can't create user data"
                    val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                    sendResponse("error", error)
                }
            } else {
                val errorMessage = "Data is invalid"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendResponse("error", error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal server error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendResponse("error", error)
        }
    }

    fun handleUpdateUserData(requestBody: String) {
        println(requestBody)
        val userData: UserData = try {
            Json.decodeFromString<UserData>(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
            return
        }
        try {
            if (UserDataValidator.userDataValidation(userData)) {
                val result = userDataService.updateUserData(userData)
                if (result) {
                    val weightHistoryResponse = WeightHistoryResponse(
                        username = userData.username,
                        weightKg = userData.weightKg,
                    )
                    sendResponse("create_new_weight", weightHistoryResponse)
                } else {
                    val errorMessage = "Can't update user data"
                    val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                    sendResponse("error", error)
                }
            } else {
                val errorMessage = "Data is invalid"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendResponse("error", error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal server error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendResponse("error", error)
        }
    }

    fun receiveUserData(username: String) {
        val userData = userDataService.getUserData(username)
        if (userData != null) {
            sendResponse("sendUserData", userData)
        } else {
            val errorMessage = "Can't get user data for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
        }
    }

    fun receiveName(username: String) {
        val userName = userDataService.getName(username)
        if (userName != null) {
            sendEvent("sendName", userName)
        } else {
            val errorMessage = "Can't get name for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
        }
    }

    fun receiveGender(username: String) {
        val userGender = userDataService.getGender(username)
        if (userGender != null) {
            sendEvent("sendGender", userGender.name)
        } else {
            val errorMessage = "Can't get gender for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
        }
    }

    fun receiveBirthDate(username: String) {
        val userBirthDate = userDataService.getBirthDate(username)
        if (userBirthDate != null) {
            sendEvent("sendBirthDate", userBirthDate.toString())
        } else {
            val errorMessage = "Can't get birthDate for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
        }
    }

    fun receiveAge(username: String) {
        val userAge = userDataService.getAge(username)
        if (userAge != null) {
            sendEvent("sendAge", userAge.toString())
        } else {
            val errorMessage = "Can't get age for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
        }
    }

    fun receiveHeight(username: String) {
        val userHeight = userDataService.getHeightCm(username)
        if (userHeight != null) {
            sendEvent("sendHeight", userHeight.toString())
        } else {
            val errorMessage = "Can't get height for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
        }
    }

    fun receiveWeight(username: String) {
        val userWeight = userDataService.getWeightKg(username)
        if (userWeight != null) {
            sendEvent("sendWeight", userWeight.toString())
        } else {
            val errorMessage = "Can't get weight for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
        }
    }

    fun receiveWeightDesire(username: String) {
        val userWeightDesire = userDataService.getWeightDesire(username)
        if (userWeightDesire != null) {
            sendEvent("sendWeightDesire", userWeightDesire.name)
        } else {
            val errorMessage = "Can't get weight-desire for $username"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendResponse("error", error)
        }
    }

    private inline fun <reified T> sendResponse(channel: String, response: T) {
        val responseJson = Json.encodeToString(response)
        sendEvent(channel, responseJson)
    }
}