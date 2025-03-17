package model.response

data class ErrorWrapper(
    val id: Int,
    val error: ErrorDto,
)
