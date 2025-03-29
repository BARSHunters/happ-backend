package service

import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Profile
import model.UserData
import model.response.SocialResponse
import model.response.ResponseWrapper
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class UserProfileService {
    private val pendingRequests = ConcurrentHashMap<UUID, Profile>()

    fun requestUserProfile(id: UUID, username: String) {
        // Initialize the profile data
        val profileData = Profile(
            username = username,
            name = "",
            friendCount = 0,
            age = 0,
            height = 0,
            weight = 0f
        )
        pendingRequests[id] = profileData

        // Request user data (name, age, height, weight)
        val userDataId = UUID.randomUUID()
        val userDataRequest = """
            {
                "id": "${userDataId}",
                "username": "${username}"
            }
        """.trimIndent()
        sendEvent("user_data:request:GetUserData", userDataRequest)
    }

    fun handleUserDataResponse(responseBody: String) {
        try {
            val response: ResponseWrapper<UserData> = Json.decodeFromString(responseBody)
            val profileData = pendingRequests[response.id] ?: return

            // Update profile with user data
            profileData.age = response.dto.age
            profileData.name = response.dto.name
            profileData.height = response.dto.height
            profileData.weight = response.dto.weight

            // Check if we have all data
            checkAndSendCompleteProfile(response.id, profileData)
        } catch (e: Exception) {
            println("Error handling user data response: ${e.message}")
        }
    }

    private fun checkAndSendCompleteProfile(id: UUID, profileData: Profile) {
        // Check if we have all required data
        if (profileData.age > 0 &&
            profileData.height > 0 &&
            profileData.weight > 0) {
            
            // Send complete profile
            val response = ResponseWrapper(id, SocialResponse(Json.encodeToString(profileData)))
            sendEvent("social:response:GetUserProfile", Json.encodeToString(response))
            
            // Clean up
            pendingRequests.remove(id)
        }
    }

    fun updateFriendCount(id: UUID, count: Int) {
        val profileData = pendingRequests[id] ?: return
        profileData.friendCount = count
        checkAndSendCompleteProfile(id, profileData)
    }
} 