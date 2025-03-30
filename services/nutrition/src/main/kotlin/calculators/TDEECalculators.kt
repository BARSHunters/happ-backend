package org.example.calculators

import org.example.dto.UserDTO
import org.example.model.Gender
import java.time.LocalDate
import java.time.Period

/**
 * Известные калькуляторы калорий
 *
 * Позволяет создать объект калькулятора зная только нужный тип и [UserDTO]
 *
 * @param calculator конструктор нужного калькулятора
 */
enum class TDEECalculatorType(val calculator: (user: UserDTO, activityIndex: Float) -> Calculator) {
    MIFFLIN({ user, activityIndex ->
        MifflinStJeor(
            user.weightKg.toUInt(),
            user.heightCm.toUInt(),
            Period.between(user.birthDate, LocalDate.now()).years.toUInt(),
            user.gender,
            activityIndex
        )
    }),
    HARRIS({ user, activityIndex ->
        HarrisBenedict(
            user.weightKg.toUInt(),
            user.heightCm.toUInt(),
            Period.between(user.birthDate, LocalDate.now()).years.toUInt(),
            user.gender,
            activityIndex
        )
    }),
    KATCH({ user, activityIndex ->
        KatchMcArdle(
            user.weightKg.toUInt(),
            activityIndex,
            user.bodyFatPercent ?:
            user.preferredBodyFatCalculator?.calculator?.invoke(user)?.calculate()?.getOrNull() ?:
            BodyFatCalculatorType.BMI.calculator(user).calculate().getOrElse { 0.0 }
        )
    }),
    FAO({ user, activityIndex ->
        FAOWHO(
            user.weightKg.toUInt(),
            Period.between(user.birthDate, LocalDate.now()).years.toUInt(),
            user.gender,
            activityIndex
        )
    })
}

/** Калькулятор считающий по формуле FAO/WHO/UNU 2001 */
class FAOWHO(
    private val weight: UInt,
    private val age: UInt,
    private val gender: Gender,
    private val activityIndex: Float,
) : Calculator {
    override fun calculate(): Result<Double> {
        val (alpha, beta) = when (gender) {
            Gender.MALE -> when (age) {
                in 0u..18u -> return Result.failure(NotImplementedError("Age under 18 y.o. not supported"))
                in 18u..30u -> 0.062 to 2.036
                in 31u..60u -> 0.034 to 3.538
                else -> 0.038 to 2.755
            }

            Gender.FEMALE -> when (age) {
                in 0u..18u -> return Result.failure(NotImplementedError("Age under 18 y.o. not supported"))
                in 18u..30u -> 0.063 to 2.896
                in 31u..60u -> 0.484 to 3.653
                else -> 0.491 to 2.459
            }
        }
        return Result.success(((alpha * weight.toDouble() + beta) * 240) * activityIndex.toDouble())
    }
}

/** Калькулятор считающий по формуле Миффлин-Сан Жеора */
class MifflinStJeor(
    private val weight: UInt,
    private val height: UInt,
    private val age: UInt,
    private val gender: Gender,
    private val activityIndex: Float,
) : Calculator {
    override fun calculate(): Result<Double> = Result.success(
        (10 * weight.toDouble() + 6.25 * height.toDouble() - 5 * age.toDouble() + when (gender) {
            Gender.MALE -> 5
            Gender.FEMALE -> -161
        }) * activityIndex
    )
}

/** Калькулятор считающий по формуле Харрис-Бенедикта */
class HarrisBenedict(
    private val weight: UInt,
    private val height: UInt,
    private val age: UInt,
    private val gender: Gender,
    private val activityIndex: Float,
) : Calculator {
    override fun calculate(): Result<Double> = Result.success(
        when (gender) {
            Gender.MALE -> (88.36 + (13.4 * weight.toDouble()) + (4.8 * height.toDouble()) - (5.7 * age.toDouble())) * activityIndex
            Gender.FEMALE -> (447.6 + (9.2 * weight.toDouble()) + (3.1 * height.toDouble()) - (4.3 * age.toDouble())) * activityIndex
        }
    )
}

/** Калькулятор считающий по формуле Кетч-МакАрдла */
class KatchMcArdle(
    private val weight: UInt,
    private val activityIndex: Float,
    private val bodyFatPercent: Double,
) : Calculator {
    override fun calculate(): Result<Double> =
        Result.success((370 + 21.6 * weight.toDouble() * (1 - bodyFatPercent / 100)) * activityIndex)
}