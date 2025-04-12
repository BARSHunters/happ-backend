package model.request

import kotlinx.serialization.Serializable
import utils.UUIDSerializer
import java.util.*

@Serializable
data class TokenDto(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val token: String,
)
