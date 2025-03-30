package org.example.dto

import kotlinx.serialization.Serializable
import org.example.utils.LocalDateSerializer
import org.example.utils.UUIDSerializer
import java.time.LocalDate
import java.util.*

/**
 * Представление запроса на получение истории КБЖУ рационов
 */
@Serializable
data class HistoryRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val login: String,
    val days: Int,
)

/**
 * Представление строки таблицы истории в запросе на получение истории КБЖУ рационов
 */
@Serializable
data class HistoryRow(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)

/**
 * Представление ответа с историей КБЖУ рационов
 */
@Serializable
data class HistoryResponseDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val rations: Map<String, HistoryRow>
)

/**
 * Представление всех данных строки истории
 */
data class HistoryFullDTO(
    val login: String,
    val date: LocalDate,
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

/**
 * Представление запроса на получение рациона за конкретную дату
 */
@Serializable
data class HistoryRequestRationByDateDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val login: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)
