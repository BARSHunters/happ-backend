package com.example.data

import kotlinx.serialization.Serializable

/**
 * Запрос от API Gateway
 * @param userId Идентификатор пользователя.
 * @param jsonWorkout JSON-строка с данными о тренировке (опционально).
 * @param trainingDate Дата тренировки (опционально).
 */
@Serializable
data class APIGatewayToActivityRequest(
    val userId: String,
    val jsonWorkout: String? = null,
    val trainingDate: String? = null,
)

@Serializable
data class ActivityDTO(
    val duration: String, // format hh:mm:ss
    val heartRates: List<HeartRate>
)

@Serializable
data class HeartRate(
    val timestamp: Long,
    val heartRate: Int
)
