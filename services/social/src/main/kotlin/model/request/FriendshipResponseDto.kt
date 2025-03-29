package model.request

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipResponseDto(
    val senderUsername: String,
    val receiverUsername: String,
    val response: String
) 