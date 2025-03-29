package org.example.calculators

import org.example.dto.UserDTO
import org.example.model.Gender

/**
 * Известные калькуляторы калорий
 *
 * Позволяет создать объект калькулятора зная только нужный тип и [UserDTO]
 *
 * @param calculator конструктор нужного калькулятора
 */
enum class TDEECalculatorType(val calculator: (user: UserDTO) -> Calculator) {
    MIFFLIN({ user -> MifflinStJeor(user.weight, user.height, user.age, user.gender, user.activityIndex) }),
    HARRIS({ user -> HarrisBenedict(user.weight, user.height, user.age, user.gender, user.activityIndex) }),
    KATCH({ user ->
        KatchMcArdle(
            user.weight,
            user.activityIndex,
            user.bodyFatPercent ?: throw NullPointerException()
        )
    }),
    FAO({ user -> FAOWHO(user.weight, user.age, user.gender, user.activityIndex) })
}

/** Калькулятор считающий по формуле FAO/WHO/UNU 2001 */
class FAOWHO(
    private val weight: UInt,
    private val age: UInt,
    private val gender: Gender,
    private val activityIndex: Float,
) : Calculator {
    override fun calculate(): Double {
        val (alpha, beta) = when (gender) {
            Gender.MALE -> when (age) {
                in 0u..18u -> throw NotImplementedError("Возраст до 18 лет не поддерживается")
                in 18u..30u -> 0.062 to 2.036
                in 31u..60u -> 0.034 to 3.538
                else -> 0.038 to 2.755
            }

            Gender.FEMALE -> when (age) {
                in 0u..18u -> throw NotImplementedError("Возраст до 18 лет не поддерживается")
                in 18u..30u -> 0.063 to 2.896
                in 31u..60u -> 0.484 to 3.653
                else -> 0.491 to 2.459
            }
        }
        return ((alpha * weight.toDouble() + beta) * 240) * activityIndex.toDouble()
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
    override fun calculate(): Double =
        (10 * weight.toDouble() + 6.25 * height.toDouble() - 5 * age.toDouble() + when (gender) {
            Gender.MALE -> 5
            Gender.FEMALE -> -161
        }) * activityIndex
}

/** Калькулятор считающий по формуле Харрис-Бенедикта */
class HarrisBenedict(
    private val weight: UInt,
    private val height: UInt,
    private val age: UInt,
    private val gender: Gender,
    private val activityIndex: Float,
) : Calculator {
    override fun calculate(): Double = when (gender) {
        Gender.MALE -> (88.36 + (13.4 * weight.toDouble()) + (4.8 * height.toDouble()) - (5.7 * age.toDouble())) * activityIndex
        Gender.FEMALE -> (447.6 + (9.2 * weight.toDouble()) + (3.1 * height.toDouble()) - (4.3 * age.toDouble())) * activityIndex
    }
}

/** Калькулятор считающий по формуле Кетч-МакАрдла */
class KatchMcArdle(
    private val weight: UInt,
    private val activityIndex: Float,
    private val bodyFatPercent: Double,
) : Calculator {
    override fun calculate(): Double = (370 + 21.6 * weight.toDouble() * (1 - bodyFatPercent / 100)) * activityIndex
}