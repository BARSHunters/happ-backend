package model.request

import kotlinx.serialization.Serializable

@Serializable
data class RequestWrapper<T>(
    val id: Int,
    val dto: T
)
