package model.response

import kotlinx.serialization.Serializable
import utils.UUIDSerializer
import java.util.*

@Serializable
data class TokenValidationResponse(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val message: String,
    val username: String
)
