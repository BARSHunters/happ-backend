package org.example.dto

import kotlinx.serialization.Serializable
import org.example.calculators.BodyFatCalculatorType
import org.example.calculators.TDEECalculatorType
import org.example.decider.Wish
import org.example.model.Gender
import org.example.utils.LocalDateSerializer
import org.example.utils.UUIDSerializer
import java.time.LocalDate
import java.util.*

/**
 * Представление запроса на генерацию рациона
 */
@Serializable
data class RationRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val login: String,
)

/**
 * Представление ответа от WeightService на запрос пожеланий пользователя
 */
@Serializable
data class WishResponseDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val wish: Wish
)

/**
 * Представление ответа от ActivityService на запрос индекса активности пользователя
 */
@Serializable
data class ActivityResponseDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val activityIndex: Float
)

/**
 * Представление запроса на данные пользователя к UserDataService
 */
@Serializable
data class UserDataRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val username: String,
)

/**
 * Представление ответа от UserDataService на запрос данных пользователя
 */
@Serializable
data class UserDataResponseDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val dto: UserDTO,
)

/**
 * Представление данных в ответе от UserDataService на запрос данных пользователя
 */
@Serializable
data class UserDTO(
    val username: String,
    val name: String,
    val age: Int,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate,
    val gender: Gender,
    val height: Int,
    val weight: Float,
    val weightDesire: Wish,

    // Для расчёта калорий по одной из формул (Кетч-МакАрдла)
    val bodyFatPercent: Double? = null,
    // поля для более точного расчёта процента жира
    val neck: UInt? = null,
    val waist: UInt? = null,
    val hips: UInt? = null,
    val sumOfSkinfolds: UInt? = null,

    // предпочтительные калькуляторы (есть значения по умолчанию)
    val preferredTDEECalculator: TDEECalculatorType? = null, // FAO по умолчанию
    val preferredBodyFatCalculator: BodyFatCalculatorType? = null // BMI по умолчанию
)


/**
 * Представление кешированной между запросами строки
 */
data class RationCacheDTO(
    val uuid: UUID,
    val login: String,
    val wish: Wish?,
    val type: MealType?,
    val activityIndex: Float?,
)

/**
 * Представление ответа с рационом
 */
@Serializable
data class RationResponseDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val dishSetDTO: DailyDishSetDTO
)


/**
 * Приемы пищи: завтрак, обед, ужин
 */
enum class MealType { BREAKFAST, LUNCH, DINNER; }

/**
 * Представление запроса на повторную генерацию рациона (замена одного приема пищи)
 */
@Serializable
data class UpdateRationRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val login: String,
    val type: MealType,
)
