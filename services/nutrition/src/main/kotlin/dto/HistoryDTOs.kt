package org.example.dto

import java.util.*


data class HistoryRequestDTO(
    val queryId: UUID,
    val login: String,
    val days: Int,
)

data class HistoryRow(
    val tdee: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)

data class HistoryResponseDTO(
    val queryId: UUID,
    val rations: List<Pair<String, HistoryRow>>
)


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