package model

import java.time.LocalDateTime
import java.util.*

data class Friendship(
    val id: UUID,
    val senderUsername: String,
    val receiverUsername: String,
    val status: FriendshipStatus,
    val createdAt: LocalDateTime
)