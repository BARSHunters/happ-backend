package org.example.decider

import org.example.dto.DailyDishSetDTO
import org.example.dto.DishDTO
import org.example.dto.MealType
import org.example.model.User
import org.example.service.DishService
import org.example.service.DishType
import org.example.service.HistoryService
import kotlin.math.abs
import kotlin.random.Random

/**
 * Пожелание пользователя об изменении/сохранении веса.
 *
 * Влияет на общее количество калорий и доли БЖУ при расчёте весов порций.
 * Доли БЖУ не финальные, в сумме должны давать 1.
 * Конкретное значение enum-а поучается из сервиса WeightHistory.
 *
 * @property tdeeIndex множитель калорий
 * @property proteinIndex множитель количества белков
 * @property fatIndex множитель количества жиров
 * @property carbsIndex множитель количества углеводов
 */
enum class Wish(val tdeeIndex: Double, val proteinIndex: Double, val fatIndex: Double, val carbsIndex: Double) {
    KEEP(1.0, 0.3, 0.25, 0.45),
    REMAIN(1.0, 0.3, 0.25, 0.45), // То же самое что и KEEP. Для совместимости.
    GAIN(1.15, 0.3, 0.25, 0.45),
    LOSS(0.85, 0.35, 0.25, 0.4);
}

/**
 * Singleton класс, содержащий методы генерации рациона.
 *
 * Класс нужен только для хранения неизменяемых свойств. В них - количество записей в таблице блюд (по типам).
 * По-хорошему, реальное количество тоже не должно меняться.
 * В остальном функции чистые.
 */
object Decider {
    /** Количество блюд с типом ANY. (в таблице в clickhouse-е на момент инициализации) */
    private val TOTAL_ANY = DishType.ANY.getTypeCount()
    /** Количество блюд и для обеда и для ужина. (в таблице в clickhouse-е на момент инициализации) */
    private val TOTAL_LD = DishType.LUNCH_OR_DINNER.getTypeCount()
    /** Количество завтраков. (в таблице в clickhouse-е на момент инициализации) */
    private val TOTAL_BREAKFAST = DishType.BREAKFAST.getTypeCount()
    /** Количество обедов. (в таблице в clickhouse-е на момент инициализации) */
    private val TOTAL_LUNCH = DishType.LUNCH.getTypeCount()
    /** Количество ужинов. (в таблице в clickhouse-е на момент инициализации) */
    private val TOTAL_DINNER = DishType.DINNER.getTypeCount()


    /**
     * Генерирует набор из 3-х блюд.
     *
     * @param user Данные о пользователе, участвующие в расчёте. Также содержит свой [калькулятор][User.calculateTDEE]
     * @param wish Пользовательское [пожелание][Wish] насчёт веса.
     */
    fun decide(user: User, wish: Wish): DailyDishSetDTO {
        val baseTDEE = user.calculateTDEE()

        val targetTDEE = baseTDEE * wish.tdeeIndex
        val targetProtein = targetTDEE * wish.proteinIndex / 4
        val targetFat = targetTDEE * wish.fatIndex / 9
        val targetCarbs = targetTDEE * wish.carbsIndex / 4

        val anyDishes = DishService.getDishesByType(
            type = DishType.ANY,
            offset = Random.nextInt(0, TOTAL_ANY - DishType.ANY.defaultMaxLimit),
            limit = Random.nextInt(DishType.ANY.defaultMinLimit, DishType.ANY.defaultMaxLimit)
        )
        val lunchOrDinner = DishService.getDishesByType(
            type = DishType.LUNCH_OR_DINNER,
            offset = Random.nextInt(0, TOTAL_LD - DishType.LUNCH_OR_DINNER.defaultMaxLimit),
            limit = Random.nextInt(DishType.LUNCH_OR_DINNER.defaultMinLimit, DishType.LUNCH_OR_DINNER.defaultMaxLimit)
        ) + anyDishes

        val breakfasts = DishService.getDishesByType(
            type = DishType.BREAKFAST,
            offset = Random.nextInt(0, TOTAL_BREAKFAST - DishType.BREAKFAST.defaultMaxLimit),
            limit = Random.nextInt(DishType.BREAKFAST.defaultMinLimit, DishType.BREAKFAST.defaultMaxLimit)
        ).map {
            if (Random.nextDouble() < 0.3 && anyDishes.isNotEmpty()) {
                anyDishes.random()
            } else it
        }.toList()
        val lunches = DishService.getDishesByType(
            type = DishType.LUNCH,
            offset = Random.nextInt(0, TOTAL_LUNCH - DishType.LUNCH.defaultMaxLimit),
            limit = Random.nextInt(DishType.LUNCH.defaultMinLimit, DishType.LUNCH.defaultMaxLimit)
        ).map {
            if (Random.nextDouble() < 0.3 && lunchOrDinner.isNotEmpty()) {
                lunchOrDinner.random()
            } else it
        }.toList()
        val dinners = DishService.getDishesByType(
            type = DishType.DINNER,
            offset = Random.nextInt(0, TOTAL_DINNER - DishType.DINNER.defaultMaxLimit),
            limit = Random.nextInt(DishType.DINNER.defaultMinLimit, DishType.DINNER.defaultMaxLimit)
        ).map {
            if (Random.nextDouble() < 0.3 && lunchOrDinner.isNotEmpty()) {
                lunchOrDinner.random()
            } else it
        }.toList()

        var bestSet: Triple<DishDTO, DishDTO, DishDTO>? = null
        var minError = Double.MAX_VALUE

        for (breakfast in breakfasts) {
            for (lunch in lunches) {
                for (dinner in dinners) {
                    val combo = Triple(breakfast, lunch, dinner)
                    val scalingFactor = targetTDEE / combo.toList().sumOf { it.tdee } // Во сколько больше раз надо взять каждого блюда
                    val adjustedCombo = adjustCombo(combo, scalingFactor)

                    val error = calculateError(adjustedCombo, targetTDEE, targetProtein, targetFat, targetCarbs)
                    if (error < minError) {
                        bestSet = adjustedCombo
                        minError = error
                    }
                }
            }
        }

        return bestSet?.let { DailyDishSetDTO(it.first, it.second, it.third) }
            ?: throw IllegalStateException("Не удалось подобрать блюда")
    }

    /**
     * Домножает веса блюд на указанный [scalingFactor]
     *
     * @param combo набор блюд для увеличения веса
     * @param scalingFactor множитель
     * @see [DishDTO.adjustWeight]
     */
    private fun adjustCombo(combo: Triple<DishDTO, DishDTO, DishDTO>, scalingFactor: Double): Triple<DishDTO, DishDTO, DishDTO> {
        val breakfast = combo.first
        val lunch = combo.second
        val dinner = combo.third

        val breakfastWeight = (breakfast.weight.toDouble() * scalingFactor).coerceAtLeast(100.0)
        val lunchWeight = (lunch.weight.toDouble() * scalingFactor).coerceAtLeast(100.0)
        val dinnerWeight = (dinner.weight.toDouble() * scalingFactor).coerceAtLeast(100.0)

        val adjustedBreakfast = breakfast.adjustWeight(breakfastWeight)
        val adjustedLunch = lunch.adjustWeight(lunchWeight)
        val adjustedDinner = dinner.adjustWeight(dinnerWeight)

        return Triple(adjustedBreakfast, adjustedLunch, adjustedDinner)
    }

    /**
     * Считает разницу между переданным набором блюд и переданными целевыми значениями.
     *
     * Подразумевается передача параметров с учётом []пожелания пользователя][Wish] и набора блюд с уже определенным весом.
     * @see adjustCombo
     */
    private fun calculateError(
        adjustCombo: Triple<DishDTO, DishDTO, DishDTO>,
        targetTDEE: Double,
        targetProtein: Double,
        targetFat: Double,
        targetCarbs: Double,
    ): Double {
        val adjustedComboList = adjustCombo.toList()

        val error = abs(adjustedComboList.sumOf { it.protein } - targetProtein) +
                abs(adjustedComboList.sumOf { it.fat } - targetFat) +
                abs(adjustedComboList.sumOf { it.carbs } - targetCarbs) +
                abs(adjustedComboList.sumOf { it.tdee } - targetTDEE)

        return error
    }

    /**
     * Обновляет одно из блюд в наборе.
     *
     * Набор должен быть уже сгенерирован и доступен в истории ([HistoryService.getTodayHistoryForUser]).
     *
     * @param user Данные о пользователе, участвующие в расчёте. Также содержит свой [калькулятор][User.calculateTDEE]
     * @param wish Пользовательское [пожелание][Wish] насчёт веса.
     * @param mealType Приём пищи, блюдо на который нужно поменять.
     */
    fun swap(user: User, wish: Wish, mealType: MealType): DailyDishSetDTO {
        val baseTDEE = user.calculateTDEE()

        val targetTDEE = baseTDEE * wish.tdeeIndex
        val targetProtein = targetTDEE * wish.proteinIndex / 4
        val targetFat = targetTDEE * wish.fatIndex / 9
        val targetCarbs = targetTDEE * wish.carbsIndex / 4

        val other = DishService.getDishesByType(
            type = DishType.ANY,
            offset = Random.nextInt(0, TOTAL_ANY - DishType.ANY.defaultMaxLimit),
            limit = Random.nextInt(DishType.ANY.defaultMinLimit, DishType.ANY.defaultMaxLimit)
        ).toMutableList()
        if (mealType == MealType.LUNCH || mealType == MealType.DINNER) {
            other += DishService.getDishesByType(
                type = DishType.LUNCH_OR_DINNER,
                offset = Random.nextInt(0, TOTAL_LD - DishType.LUNCH_OR_DINNER.defaultMaxLimit),
                limit = Random.nextInt(
                    DishType.LUNCH_OR_DINNER.defaultMinLimit,
                    DishType.LUNCH_OR_DINNER.defaultMaxLimit
                )
            )
        }

        val meals = DishService.getDishesByType(
            type = DishType.valueOf(mealType.name),
            offset = Random.nextInt(
                0,
                DishType.valueOf(mealType.name).getTypeCount() - DishType.valueOf(mealType.name).defaultMaxLimit
            ),
            limit = Random.nextInt(
                DishType.valueOf(mealType.name).defaultMinLimit,
                DishType.valueOf(mealType.name).defaultMaxLimit
            )
        ).map {
            if (Random.nextDouble() < 0.3 && other.isNotEmpty()) {
                other.random()
            } else it
        }.toList()

        val historyRow = HistoryService.getTodayHistoryForUser(user.login)

        var breakfast = DishService.getDishById(historyRow.breakfast)
        var lunch = DishService.getDishById(historyRow.lunch)
        var dinner = DishService.getDishById(historyRow.dinner)

        var bestSet: Triple<DishDTO, DishDTO, DishDTO>? = null
        var minError = Double.MAX_VALUE

        for (meal in meals) {
            when (mealType) {
                MealType.BREAKFAST -> {
                    breakfast = meal
                }
                MealType.LUNCH -> {
                    lunch = meal
                }
                else -> {
                    dinner = meal
                }
            }

            val combo = Triple(breakfast, lunch, dinner)
            val scalingFactor = targetTDEE / combo.toList().sumOf { it.tdee } // Во сколько больше раз надо взять каждого блюда
            val adjustedCombo = adjustCombo(combo, scalingFactor)

            val error = calculateError(adjustedCombo, targetTDEE, targetProtein, targetFat, targetCarbs)
            if (error < minError) {
                bestSet = adjustedCombo
                minError = error
            }
        }

        return bestSet?.let { DailyDishSetDTO(it.first, it.second, it.third) }
            ?: throw IllegalStateException("Не удалось подобрать блюда")
    }
}
