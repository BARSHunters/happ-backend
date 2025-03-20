package model.response

import kotlinx.serialization.Serializable

@Serializable
data class ResponseWrapper<T>(
    val id: Int,
    val dto: T
)
