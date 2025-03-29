package org.example.model

import org.example.calculators.BodyFatCalculatorType
import org.example.calculators.Calculator
import org.example.calculators.TDEECalculatorType
import org.example.dto.UserDTO

enum class Gender { MALE, FEMALE }

/**
 * Модель пользователя.
 *
 * Содержит информацию и инструменты для генерации (обновления) рациона ([Decider][org.example.decider.Decider])
 *
 * @property tdeeCalculator калькулятор для расчёта калорий. Для ожидаемого результата должен быть одним из типов [TDEECalculatorType]
 * @property tdeeCalculator калькулятор для расчёта процента жира (нужен для некоторых калькуляторов калорий).
 *  Для ожидаемого результата должен быть одним из типов [BodyFatCalculatorType]
 */
class User(
    val login: String,
    val weight: UInt,
    val height: UInt,
    val age: UInt,
    val gender: Gender,
    val activityIndex: Float,
    private val tdeeCalculator: Calculator,
    private val bodyFatCalculator: Calculator,
) {
    /**
     * @return Стандартное количество калорий для человека с такими параметрами, достаточное для подержания веса.
     */
    fun calculateTDEE(): Double {
        return tdeeCalculator.calculate()
    }

    /**
     * @return Предполагаемый процент подкожного жира для человека с такими параметрами.
     */
    fun calculateBodyFat(): Double {
        return bodyFatCalculator.calculate()
    }

    constructor(dto: UserDTO) : this(
        dto.login,
        dto.weight,
        dto.height,
        dto.age,
        dto.gender,
        dto.activityIndex,
        dto.preferredTDEECalculator?.calculator?.invoke(dto) ?: TDEECalculatorType.FAO.calculator(dto),
        dto.preferredBodyFatCalculator?.calculator?.invoke(dto) ?: BodyFatCalculatorType.BMI.calculator(dto)
    )
}