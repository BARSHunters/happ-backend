package org.example.dto

import java.util.*

/**
 * Представление запроса на получение истории КБЖУ рационов
 */
data class HistoryRequestDTO(
    val queryId: UUID,
    val login: String,
    val days: Int,
)

/**
 * Представление строки таблицы истории в запросе на получение истории КБЖУ рационов
 */
data class HistoryRow(
    val tdee: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)

/**
 * Представление ответа с историей КБЖУ рационов
 */
data class HistoryResponseDTO(
    val queryId: UUID,
    val rations: List<Pair<String, HistoryRow>>
)

/**
 * Представление всех данных строки истории
 */
data class HistoryFullDTO(
    val login: String,
    val date: Date,
    val breakfast: Long,
    val breakfastWeight: Int,
    val lunch: Long,
    val lunchWeight: Int,
    val dinner: Long,
    val dinnerWeight: Int,
    val totalTDEE: Double,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
)