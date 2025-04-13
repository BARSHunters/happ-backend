package com.example.data

import kotlinx.serialization.Serializable

/**
 * Запрос от API Gateway
 * @property username Идентификатор пользователя.
 * @property weightControlWish Пожелание по контролю веса
 */
@Serializable
data class APIGatewayToWeightHistoryRequest(
    val username: String,
    val weightControlWish: WeightDesire?,
)

/**
 * Ответ от WeightHistoryService для API Gateway
 * @property username Идентификатор пользователя.
 * @property weightHistory Список записей с историей веса (ключ - дата/время, значение - вес)
 */
@Serializable
data class WeightHistoryResponse(
    val username: String,
    val weightHistory: Map<String, Float>,
)
