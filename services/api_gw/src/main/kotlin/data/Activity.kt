package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class APIGatewayToActivityRequest(
    val username: String,
    val jsonWorkout: String? = null,
    val startTrainingDate: String? = null,
    val endTrainingDate: String? = null,
)

@Serializable
data class ActivityDTO(
    val duration: String, // format hh:mm:ss
    val heartRates: List<HeartRate>,

)

@Serializable
data class HeartRate(
    val timestamp: Long,
    val heartRate: Int
)
