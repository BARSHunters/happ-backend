package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class FriendshipRequestDTO(
    val senderUsername: String,
    val receiverUsername: String
)

// response must be one of following:
// accept/reject
@Serializable
data class FriendshipResponseDTO(
    val senderUsername: String,
    val receiverUsername: String,
    val response: String
)
