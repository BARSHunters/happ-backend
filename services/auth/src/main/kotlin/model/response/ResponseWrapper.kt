package model.response

data class ResponseWrapper<T>(
    val id: Int,
    val dto: T
)
