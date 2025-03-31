package model.request

import kotlinx.serialization.Serializable
import utils.UUIDSerializer
import java.util.*

@Serializable
data class GetterDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val username: String,
)
