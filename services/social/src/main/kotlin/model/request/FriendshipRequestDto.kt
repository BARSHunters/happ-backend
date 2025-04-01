package model.request

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipRequestDto(
    val senderUsername: String,
    val receiverUsername: String
) 