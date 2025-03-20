package model.response

import kotlinx.serialization.Serializable
import model.ErrorType

@Serializable
data class ErrorDto(
    val errorType: ErrorType,
    val errorMessage: String,
)
