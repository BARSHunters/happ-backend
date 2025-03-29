package org.example.dto

import org.example.calculators.BodyFatCalculatorType
import org.example.calculators.TDEECalculatorType
import org.example.decider.Wish
import org.example.model.Gender
import java.util.*

data class RationRequestDTO(
    val queryId: UUID,
    val login: String,
)

data class WishResponseDTO(
    val queryId: UUID,
    val wish: Wish
)

data class UserDataRequestDTO(
    val queryId: UUID,
    val login: String,
)

data class UserDTO(
    val queryId: UUID,

    val login: String,
    val weight: UInt,
    val height: UInt,
    val age: UInt,
    val gender: Gender,
    val activityIndex: Float,

    val bodyFatPercent: Double?,

    // поля для более точного расчёта процента жира
    val neck: UInt?,
    val waist: UInt?,
    val hips: UInt?,
    val sumOfSkinfolds: UInt?,

    // предпочтительные калькуляторы
    val preferredTDEECalculator: TDEECalculatorType?,
    val preferredBodyFatCalculator: BodyFatCalculatorType?
)

data class RationCacheDTO(
    val queryId: UUID,
    val login: String,
    val wish: Wish?,
    val type: MealType?,
)

data class RationResponseDTO(
    val queryId: UUID,
    val dishSetDTO: DailyDishSetDTO
)


enum class MealType { BREAKFAST, LUNCH, DINNER; }

data class UpdateRationRequestDTO(
    val queryId: UUID,
    val login: String,
    val type: MealType,
)
