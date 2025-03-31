package model.response

import kotlinx.serialization.Serializable

@Serializable
data class WeightHistoryResponse(
    val username: String,
    val weightKg: Float
)