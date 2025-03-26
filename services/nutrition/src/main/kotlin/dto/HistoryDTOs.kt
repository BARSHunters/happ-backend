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