package com.example.data

import kotlinx.serialization.Serializable
import serializers.LocalDateSerializer
import serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class RationRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val login: String,
)

@Serializable
data class UpdateRationRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val login: String,
    val type: MealType,
)

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER
}

@Serializable
data class HistoryRequestRationByDateDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val login: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)
