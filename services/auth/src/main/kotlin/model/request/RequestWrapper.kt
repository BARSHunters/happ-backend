package model.request

import kotlinx.serialization.Serializable
import utils.UUIDSerializer
import java.util.*

@Serializable
data class RequestWrapper<T>(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val dto: T
)
