package controller

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ErrorType
import model.request.FriendshipRequestDto
import model.request.RequestWrapper
import model.response.*
import service.SocialService
import java.util.*

class SocialController(private val socialService: SocialService) {

    fun handleProposeFriendship(requestBody: String) {
        println("Propose friendship request: $requestBody")
        val request: RequestWrapper<FriendshipRequestDto> = try {
            Json.decodeFromString(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendError(UUID.fromString("-1"), error)
            return
        }

        try {
            val success = socialService.proposeFriendship(request.uuid, request.dto)
            if (success) {
                val notification = Notification(
                    NotificationType.FRIEND_REQUEST, request.dto.receiverUsername,
                    NotificationPayload.FriendRequestPayload(friendName = request.dto.senderUsername)
                )
                println("Sending notification to the notify service $notification")
                sendEvent("notify:request:SendNotification", Json.encodeToString(notification))
            } else {
                val errorMessage = "Failed to send friend request"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendError(request.uuid, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.uuid, error)
        }
    }

    fun handleRespondToFriendship(requestBody: String) {
        println("Respond to friendship request: $requestBody")
        val request: RequestWrapper<FriendshipResponseDto> = try {
            Json.decodeFromString(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendError(UUID.fromString("-1"), error)
            return
        }

        try {
            val success = socialService.respondToFriendship(request.uuid, request.dto)
            if (!success) {
                val errorMessage = "Failed to ${request.dto.response} friend request"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendError(request.uuid, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.uuid, error)
        }
    }

    fun handleGetUserProfile(requestBody: String) {
        println("Get user profile request: $requestBody")
        val request: RequestWrapper<String> = try {
            Json.decodeFromString(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendError(UUID.fromString("-1"), error)
            return
        }

        try {
            socialService.getUserProfile(request.uuid, request.dto)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.uuid, error)
        }
    }

    fun handleGetFriendsList(requestBody: String) {
        println("Get friend list request: $requestBody")
        val request: RequestWrapper<String> = try {
            Json.decodeFromString(requestBody)
        } catch (e: SerializationException) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendError(UUID.fromString("-1"), error)
            return
        }

        try {
            val friendList = socialService.getFriendsList(request.dto)
            val friendsListResponse = FriendsListResponse(friendList, friendList.size)
            val response = ResponseWrapper(request.uuid, friendsListResponse)
            sendEvent("social:response:GetFriendsList", Json.encodeToString(response))
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.uuid, error)
        }
    }

    fun handleGetFriendRequests(requestBody: String) {
        println("Get friend requests: $requestBody")
        val request: RequestWrapper<String> = try {
            Json.decodeFromString(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Invalid JSON format"
            val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
            sendError(UUID.fromString("-1"), error)
            return
        }
        try {
            val pendingFriends = socialService.getFriendshipRequests(request.dto)
            val friendsListResponse = FriendsListResponse(pendingFriends, pendingFriends.size)
            val response = ResponseWrapper(request.uuid, friendsListResponse)
            sendEvent("social:response:GetFriendsRequests", Json.encodeToString(response))
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.uuid, error)
            return
        }
    }

    private fun sendError(id: UUID, dto: ErrorDto) {
        val response = ResponseWrapper(id, dto)
        val responseJson = Json.encodeToString(response)
        sendEvent("error", responseJson)
    }
} 