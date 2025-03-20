package model.response

import kotlinx.serialization.Serializable

@Serializable
data class ErrorWrapper(
    val id: Int,
    val error: ErrorDto,
)
