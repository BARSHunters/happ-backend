package model.response

import kotlinx.serialization.Serializable
import model.ErrorType

@Serializable
data class ErrorDto(
    val type: ErrorType,
    val message: String
) 