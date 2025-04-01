package com.example.data

import java.time.LocalDate
import java.util.*

data class RationRequestDTO(
    val id: UUID,
    val login: String,
)

data class UpdateRationRequestDTO(
    val id: UUID,
    val login: String,
    val type: MealType,
)

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER
}

data class HistoryRequestRationByDateDTO(
    val id: UUID,
    val login: String,
    val date: LocalDate,
)
