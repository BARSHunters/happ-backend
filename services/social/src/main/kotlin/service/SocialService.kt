package service

import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.FriendshipStatus
import model.request.FriendshipRequestDto
import model.response.FriendshipResponseDto
import model.response.ResponseWrapper
import model.response.SocialResponse
import repository.FriendshipRepository
import java.util.*

class SocialService(
    private val friendshipRepository: FriendshipRepository,
    private val userProfileService: UserProfileService
) {

    fun proposeFriendship(id: UUID, request: FriendshipRequestDto): Boolean {
        // Check if friendship already exists
        val existingFriendship = friendshipRepository.findFriendship(
            request.senderUsername,
            request.receiverUsername
        )

        if (existingFriendship != null) {
            return false
        }

        // Create new friendship request
        val success = friendshipRepository.createFriendship(
            request.senderUsername,
            request.receiverUsername
        )

        if (success) {
            // Send notification to receiver
            val notification = SocialResponse(
                message = "${request.senderUsername} sent you a friend request"
            )
            val response = ResponseWrapper(id, notification)
            sendEvent(
                "notification:request:SendNotification",
                Json.encodeToString(response)
            )
        }

        return success
    }

    fun respondToFriendship(id: UUID, request: FriendshipResponseDto): Boolean {
        val friendship = friendshipRepository.findFriendship(
            request.senderUsername,
            request.receiverUsername
        ) ?: return false

        if (friendship.status != FriendshipStatus.PENDING) {
            return false
        }

        val newStatus = when (request.response.lowercase()) {
            "accept" -> FriendshipStatus.ACCEPTED
            "reject" -> FriendshipStatus.REJECTED
            else -> return false
        }

        val success = friendshipRepository.updateFriendshipStatus(friendship.id, newStatus)

        if (success) {
            // Send notification to sender
            val notification = SocialResponse(
                message = "${request.receiverUsername} ${request.response}ed your friend request"
            )
            val response = ResponseWrapper(id, Json.encodeToString(notification))
            sendEvent(
                "notification:request:SendNotification",
                Json.encodeToString(response)
            )
        }

        return success
    }

    fun getUserProfile(id: UUID, username: String) {
        // Get friend count from repository
        val friends = friendshipRepository.getFriends(username)
        // Request profile data
        userProfileService.requestUserProfile(id, username, friends.size)
    }

    fun getFriendsList(username: String): List<String> {
        val friends = friendshipRepository.getFriends(username)
        return friends
    }
}