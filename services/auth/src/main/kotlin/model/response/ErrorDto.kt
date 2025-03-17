package model.response

import model.ErrorType

data class ErrorDto(
    val errorType: ErrorType,
    val errorMessage: String,
)
