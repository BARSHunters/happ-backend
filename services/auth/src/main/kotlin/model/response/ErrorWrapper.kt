package model.response

import kotlinx.serialization.Serializable
import utils.UUIDSerializer
import java.util.*

@Serializable
data class ErrorWrapper(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val error: ErrorDto,
)
