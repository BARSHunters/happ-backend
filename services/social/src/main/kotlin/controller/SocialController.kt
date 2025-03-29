package controller

import keydb.sendEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.ErrorType
import model.request.RequestWrapper
import model.request.FriendshipRequestDto
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
            val success = socialService.proposeFriendship(request.id, request.dto)
            if (success) {
                val response = SocialResponse("Friend request sent successfully")
                sendResponse("social:response:ProposeFriendship", request.id, response)
            } else {
                val errorMessage = "Failed to send friend request"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendError(request.id, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.id, error)
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
            val success = socialService.respondToFriendship(request.id, request.dto)
            if (success) {
                val response = SocialResponse("Friend request ${request.dto.response}ed successfully")
                sendResponse("social:response:RespondToFriendship", request.id, response)
            } else {
                val errorMessage = "Failed to ${request.dto.response} friend request"
                val error = ErrorDto(ErrorType.BAD_REQUEST, errorMessage)
                sendError(request.id, error)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.id, error)
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
            socialService.getUserProfile(request.id, request.dto)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.id, error)
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
            if (friendList.isNotEmpty()) {
                val friendsListResponse = FriendsListResponse(friendList, friendList.size)
                val response = ResponseWrapper(request.id, friendsListResponse)
                sendEvent("social:response:GetFriendsList", Json.encodeToString(response))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = "Internal Server Error"
            val error = ErrorDto(ErrorType.INTERNAL_SERVER_ERROR, errorMessage)
            sendError(request.id, error)
        }
    }

    private fun sendResponse(channel: String, id: UUID, response: SocialResponse) {
        val responseWrapper = ResponseWrapper(id = id, dto = response)
        val responseJson = Json.encodeToString(responseWrapper)
        sendEvent(channel, responseJson)
    }

    private fun sendError(id: UUID, dto: ErrorDto) {
        val response = ResponseWrapper(id, dto)
        val responseJson = Json.encodeToString(response)
        sendEvent("error", responseJson)
    }
} 