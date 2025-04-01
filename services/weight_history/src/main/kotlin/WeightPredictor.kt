import utils.Gender
import utils.WeightDesire
import kotlin.math.max
import kotlin.math.pow

/**
 * Класс для прогнозирования веса.
 * @property gender Пол пользователя.
 * @property age Возраст пользователя.
 * @property height Рост пользователя.
 * @property goal Цель по контролю веса.
 */
class WeightPredictor(
    internal val gender: Gender,
    internal val age: Int,
    internal val height: Int,
    internal val goal: WeightDesire,
) {
    internal val weightHistory = mutableListOf<Double>()
    internal val calorieIntakeHistory = mutableListOf<Double>()
    internal val caloriesBurnedHistory = mutableListOf<Double>()

    internal fun Double.pow(exp: Int): Double = this.pow(exp.toDouble())

    internal fun calculateBMR(weight: Double): Double {
        return if (gender == Gender.MALE) {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }
    }

    /**
     * Добавляет запись о весе, потреблении и расходе калорий.
     * @param weight Вес пользователя.
     * @param caloriesIntake Потребление калорий.
     * @param caloriesBurned Расход калорий.
     */
    fun addRecord(
        weight: Double,
        caloriesIntake: Double,
        caloriesBurned: Double,
    ) {
        weightHistory.add(weight)
        calorieIntakeHistory.add(caloriesIntake)
        caloriesBurnedHistory.add(caloriesBurned)
    }

    /**
     * Прогнозирует следующий вес пользователя.
     * @return Прогнозируемый вес.
     */
    fun predictNextWeight(): Double {
        if (weightHistory.isEmpty()) return 0.0

        val currentWeight = weightHistory.last()
        val lastCaloriesIntake = calorieIntakeHistory.last()
        val lastCaloriesBurned = caloriesBurnedHistory.last()
        val bmr = calculateBMR(currentWeight)
        val tef = 0.1 * lastCaloriesIntake

        val goalAdjustment =
            when (goal) {
                WeightDesire.LOSS -> -500.0
                WeightDesire.GAIN -> 500.0
                else -> 0.0
            }

        val empirical =
            currentWeight +
                (lastCaloriesIntake - tef - bmr - lastCaloriesBurned + goalAdjustment) / 7700

        val regression =
            if (weightHistory.size >= 2) { // логичнее чем >= 4, ведь берём только last()
                linearRegressionPrediction()
            } else {
                empirical
            }

        val alpha = max(1.0 - weightHistory.size / 20.0, 0.1)
        return alpha * empirical + (1 - alpha) * regression
    }

    /**
     * Прогнозирует вес с использованием линейной регрессии.
     * @return Прогнозируемый вес.
     */
    internal fun linearRegressionPrediction(): Double {
        val n = weightHistory.size
        val x =
            List(n) { i ->
                (
                    calorieIntakeHistory[i] * 0.9 - caloriesBurnedHistory[i] -
                        calculateBMR(weightHistory[i])
                ) / 7700
            }
        val y = weightHistory

        val xMean = x.average()
        val yMean = y.average()
        val numerator =
            x.zip(y).sumOf { (xi, yi) ->
                (xi - xMean) * (yi - yMean)
            }

        val denominator = x.sumOf { (it - xMean).pow(2) }

        return if (denominator == 0.0) {
            yMean
        } else {
            val beta = numerator / denominator
            val alpha = yMean - beta * xMean
            alpha + beta * x.last()
        }
    }
}
