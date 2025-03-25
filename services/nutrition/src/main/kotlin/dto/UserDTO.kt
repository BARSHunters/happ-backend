package org.example.dto

import org.example.calculators.BodyFatCalculatorType
import org.example.calculators.TDEECalculatorType
import org.example.model.Gender

data class UserDTO(
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