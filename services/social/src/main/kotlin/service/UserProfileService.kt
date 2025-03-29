package service

import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ErrorType
import model.Profile
import model.UserData
import model.response.ErrorDto
import model.response.ResponseWrapper
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UserProfileService {
    private val pendingRequests = ConcurrentHashMap<UUID, Profile>()

    fun requestUserProfile(id: UUID, username: String, friendCount: Int) {
        // Create a future to wait for the complete profile

        pendingRequests[id] = Profile(
            username = username,
            name = "",
            friendCount = friendCount,
            age = 0,
            height = 0,
            weight = 0f
        )

        // Request user data (name, age, height, weight)
        val userDataRequest = """
            {
                "id": "$id",
                "username": "$username"
            }
        """.trimIndent()
        sendEvent("user_data:request:UserData", userDataRequest)
    }

    fun handleUserDataResponse(responseBody: String) {
        try {
            println("Received user data response: $responseBody")
            val response: ResponseWrapper<UserData> = try {
                Json.decodeFromString(responseBody)
            } catch(e: Exception) {
                e.printStackTrace()
                val errorMessage = "Invalid JSON format"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                val resp = ResponseWrapper(UUID.fromString("-1"), error)
                val responseJson = Json.encodeToString(resp)
                sendEvent("error", responseJson)
                return
            }
            val profile = pendingRequests[response.id] ?: return
            // Create profile from user data
            profile.name = response.dto.name
            profile.age = response.dto.age
            profile.height = response.dto.height
            profile.weight = response.dto.weight
            pendingRequests.remove(response.id)
            val responseWrapper = ResponseWrapper(response.id, profile)
            val json = Json.encodeToString(responseWrapper)
            sendEvent("social:response:GetUserProfile", json)
        } catch (e: Exception) {
            println("Error handling user data response: ${e.message}")
            e.printStackTrace()
        }
    }
} 