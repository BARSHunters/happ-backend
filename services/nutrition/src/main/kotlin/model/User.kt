package org.example.model

import org.example.calculators.BodyFatCalculatorType
import org.example.calculators.Calculator
import org.example.calculators.TDEECalculatorType
import org.example.dto.UserDTO

enum class Gender { MALE, FEMALE }

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
    fun calculateTDEE(): Double {
        return tdeeCalculator.calculate()
    }

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