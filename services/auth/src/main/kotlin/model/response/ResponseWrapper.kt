package model.response

import kotlinx.serialization.Serializable
import utils.UUIDSerializer
import java.util.*

@Serializable
data class ResponseWrapper<T>(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val dto: T
)
