package org.example.decider

import org.example.dto.DailyDishSetDTO
import org.example.dto.DishDTO
import org.example.model.User
import org.example.service.DishService
import org.example.service.DishType
import kotlin.math.abs
import kotlin.random.Random

enum class Wish(val tdeeIndex: Double, val proteinIndex: Double, val fatIndex: Double, val carbsIndex: Double) {
    KEEP(1.0, 0.3, 0.25, 0.45),
    GAIN(1.15, 0.3, 0.25, 0.45),
    LOSS(0.85, 0.35, 0.25, 0.4);
}

object Decider {
    private val TOTAL_ANY = DishType.ANY.getTypeCount()
    private val TOTAL_LD = DishType.LUNCH_OR_DINNER.getTypeCount()
    private val TOTAL_BREAKFAST = DishType.BREAKFAST.getTypeCount()
    private val TOTAL_LUNCH = DishType.LUNCH.getTypeCount()
    private val TOTAL_DINNER = DishType.DINNER.getTypeCount()

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
        ).map { if (Random.nextDouble() < 0.3 && anyDishes.isNotEmpty()) { anyDishes.random() } else it }.toList()
        val lunches = DishService.getDishesByType(
            type = DishType.LUNCH,
            offset = Random.nextInt(0, TOTAL_LUNCH - DishType.LUNCH.defaultMaxLimit),
            limit = Random.nextInt(DishType.LUNCH.defaultMinLimit, DishType.LUNCH.defaultMaxLimit)
        ).map { if (Random.nextDouble() < 0.3 && lunchOrDinner.isNotEmpty()) { lunchOrDinner.random() } else it }.toList()
        val dinners = DishService.getDishesByType(
            type = DishType.DINNER,
            offset = Random.nextInt(0, TOTAL_DINNER - DishType.DINNER.defaultMaxLimit),
            limit = Random.nextInt(DishType.DINNER.defaultMinLimit, DishType.DINNER.defaultMaxLimit)
        ).map { if (Random.nextDouble() < 0.3 && lunchOrDinner.isNotEmpty()) { lunchOrDinner.random() } else it }.toList()

        var bestSet: Triple<DishDTO, DishDTO, DishDTO>? = null
        var minError = Double.MAX_VALUE

        for (breakfast in breakfasts) {
            for (lunch in lunches) {
                for (dinner in dinners) {
                    val combo = listOf(breakfast, lunch, dinner)

                    val scalingFactor = targetTDEE / combo.sumOf { it.tdee } // Во сколько больше раз надо взять каждого блюда
                    val breakfastWeight = (breakfast.weight.toDouble() * scalingFactor).coerceAtLeast(100.0)
                    val lunchWeight = (lunch.weight.toDouble() * scalingFactor).coerceAtLeast(100.0)
                    val dinnerWeight = (dinner.weight.toDouble() * scalingFactor).coerceAtLeast(100.0)

                    val adjustedBreakfast = breakfast.adjustWeight(breakfastWeight)
                    val adjustedLunch = lunch.adjustWeight(lunchWeight)
                    val adjustedDinner = dinner.adjustWeight(dinnerWeight)

                    val totalProtein = combo.sumOf { it.protein }
                    val totalFat = combo.sumOf { it.fat }
                    val totalCarbs = combo.sumOf { it.carbs }
                    val totalTDEE = combo.sumOf { it.tdee }

                    val error = abs(totalProtein - targetProtein) +
                            abs(totalFat - targetFat) +
                            abs(totalCarbs - targetCarbs) +
                            abs(totalTDEE - targetTDEE)

                    if (error < minError) {
                        bestSet = Triple(adjustedBreakfast, adjustedLunch, adjustedDinner)
                        minError = error
                    }
                }
            }
        }

        return bestSet?.let { DailyDishSetDTO(it.first, it.second, it.third) }
            ?: throw IllegalStateException("Не удалось подобрать блюда")
    }

    private fun DishDTO.adjustWeight(newWeight: Double): DishDTO {
        val factor = newWeight / 100.0
        return this.copy(
            weight = newWeight.toUInt(),
            tdee = this.tdee * factor,
            protein = this.protein * factor,
            fat = this.fat * factor,
            carbs = this.carbs * factor
        )
    }
}
