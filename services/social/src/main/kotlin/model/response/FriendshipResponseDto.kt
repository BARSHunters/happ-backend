package social.model.request

import kotlinx.serialization.Serializable
import utils.UUIDSerializer
import java.util.*

@Serializable
data class FriendshipResponseDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val senderUsername: String,
    val receiverUsername: String,
    val response: String
) 