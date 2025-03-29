package org.example.dto

import org.example.calculators.BodyFatCalculatorType
import org.example.calculators.TDEECalculatorType
import org.example.decider.Wish
import org.example.model.Gender
import java.time.LocalDate
import java.util.*

/**
 * Представление запроса на генерацию рациона
 */
data class RationRequestDTO(
    val id: UUID,
    val login: String,
)

/**
 * Представление ответа от WeightService на запрос пожеланий пользователя
 */
data class WishResponseDTO(
    val id: UUID,
    val wish: Wish
)

/**
 * Представление запроса на данные пользователя к UserDataService
 */
data class UserDataRequestDTO(
    val id: UUID,
    val username: String,
)

/**
 * Представление ответа от UserDataService на запрос данных пользователя
 */
data class UserDataResponseDTO(
    val id: UUID,
    val dto: UserDTO,
)

/**
 * Представление данных в ответе от UserDataService на запрос данных пользователя
 */
data class UserDTO(
    val username: String,
    val name: String,
    val birthDate: LocalDate,
    val gender: Gender,
    val heightCm: Int,
    val weightKg: Float,
    val weightDesire: Wish,

    // точно нужно
    val activityIndex: Float,

    // Для расчёта калорий по одной из формул (Кетч-МакАрдла)
    val bodyFatPercent: Double?,
    // поля для более точного расчёта процента жира
    val neck: UInt?,
    val waist: UInt?,
    val hips: UInt?,
    val sumOfSkinfolds: UInt?,

    // предпочтительные калькуляторы (есть значения по умолчанию)
    val preferredTDEECalculator: TDEECalculatorType?, // FAO по умолчанию
    val preferredBodyFatCalculator: BodyFatCalculatorType? // BMI по умолчанию
)


/**
 * Представление кешированной между запросами строки
 */
data class RationCacheDTO(
    val id: UUID,
    val login: String,
    val wish: Wish?,
    val type: MealType?,
)

/**
 * Представление ответа со сгенерированным рационом
 */
data class RationResponseDTO(
    val id: UUID,
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
    val id: UUID,
    val login: String,
    val type: MealType,
)
