package org.example.calculators

import org.example.dto.UserDTO
import org.example.model.Gender
import kotlin.math.ln
import java.time.LocalDate
import java.time.Period

/**
 * Известные калькуляторы процента жира
 *
 * Позволяет создать объект калькулятора зная только нужный тип и [UserDTO]
 *
 * @param calculator конструктор нужного калькулятора
 */
@Suppress("unused")
enum class BodyFatCalculatorType(val calculator: (user: UserDTO) -> Calculator) {
    YMCA({ user -> YMCA(user.waist?.toDouble() ?: throw NullPointerException(), user.heightCm.toDouble()) }),
    USNavy({ user ->
        USNavy(
            user.gender,
            user.waist?.toDouble() ?: throw NullPointerException(),
            user.neck?.toDouble() ?: throw NullPointerException(),
            user.heightCm.toDouble(),
            user.hips?.toDouble() ?: throw NullPointerException()
        )
    }),
    JacksonPollock({ user ->
        JacksonPollock(
            user.gender,
            user.sumOfSkinfolds?.toDouble() ?: throw NullPointerException(),
            Period.between(user.birthDate, LocalDate.now()).years.toDouble()
        )
    }),
    BMI({ user ->
        BMI(
            user.weightKg.toDouble(),
            user.heightCm.toDouble(),
            Period.between(user.birthDate, LocalDate.now()).years.toDouble(),
            user.gender
        )
    })
}

/** Калькулятор считающий по формуле YMCA (по обхвату талии) */
class YMCA(
    private val waist: Double,
    private val height: Double
) : Calculator {
    override fun calculate(): Result<Double> {
        return Result.success((86.010 * ln(waist) - 70.041 * ln(height) + 36.76))
    }
}

/** Калькулятор считающий по формуле US Navy (по обхвату талии, шеи и бедер) */
class USNavy(
    private val gender: Gender,
    private val waist: Double,
    private val neck: Double,
    private val height: Double,
    private val hips: Double? = null
) : Calculator {
    override fun calculate(): Result<Double> {
        return if (gender == Gender.MALE) {
            Result.success(86.010 * ln(waist - neck) - 70.041 * ln(height) + 36.76)
        } else {
            return if (hips != null) Result.success(163.205 * ln(waist + hips - neck) - 97.684 * ln(height) - 78.387)
            else Result.failure(IllegalArgumentException("Для женщин необходимо указать окружность бедер"))
        }
    }
}

/** Калькулятор считающий по формуле Джексона-Поллака (по кожным складкам) */
class JacksonPollock(
    private val gender: Gender,
    private val sumOfSkinfolds: Double,
    private val age: Double
) : Calculator {
    override fun calculate(): Result<Double> {
        val bodyDensity = if (gender == Gender.MALE) {
            1.10938 - (0.0008267 * sumOfSkinfolds) + (0.0000016 * sumOfSkinfolds * sumOfSkinfolds) - (0.0002574 * age)
        } else {
            1.0994921 - (0.0009929 * sumOfSkinfolds) + (0.0000023 * sumOfSkinfolds * sumOfSkinfolds) - (0.0001392 * age)
        }
        return Result.success((495 / bodyDensity) - 450)
    }
}

/** Калькулятор считающий по формуле BMI (через индекс массы тела) */
class BMI(
    private val weight: Double,
    private val height: Double,
    private val age: Double,
    private val gender: Gender
) : Calculator {
    override fun calculate(): Result<Double> {
        val bmi = weight / (height * height)
        val sexFactor = if (gender == Gender.MALE) 10.8 else 0.0
        return Result.success((1.20 * bmi) + (0.23 * age) - sexFactor - 5.4)
    }
}