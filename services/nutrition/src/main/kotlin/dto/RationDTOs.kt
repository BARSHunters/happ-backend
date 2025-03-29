package org.example.dto

import org.example.calculators.BodyFatCalculatorType
import org.example.calculators.TDEECalculatorType
import org.example.decider.Wish
import org.example.model.Gender
import java.util.*

/**
 * Представление запроса на генерацию рациона
 */
data class RationRequestDTO(
    val queryId: UUID,
    val login: String,
)

/**
 * Представление ответа от WeightService на запрос пожеланий пользователя
 */
data class WishResponseDTO(
    val queryId: UUID,
    val wish: Wish
)

/**
 * Представление запроса на данные пользователя к UserDataService
 */
data class UserDataRequestDTO(
    val queryId: UUID,
    val login: String,
)

/**
 * Представление ответа от UserDataService на запрос данных пользователя
 */
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

/**
 * Представление кешированной между запросами строки
 */
data class RationCacheDTO(
    val queryId: UUID,
    val login: String,
    val wish: Wish?,
    val type: MealType?,
)

/**
 * Представление ответа со сгенерированным рационом
 */
data class RationResponseDTO(
    val queryId: UUID,
    val dishSetDTO: DailyDishSetDTO
)


/**
 * Приемы пищи: завтрак, обед, ужин
 */
enum class MealType { BREAKFAST, LUNCH, DINNER; }

/**
 * Представление запроса на повторную генерацию рациона (замена одного приема пищи)
 */
data class UpdateRationRequestDTO(
    val queryId: UUID,
    val login: String,
    val type: MealType,
)
