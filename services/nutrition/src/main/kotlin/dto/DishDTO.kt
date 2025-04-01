package org.example.dto

import kotlinx.serialization.Serializable

/**
 * Представление одного блюда в рационе
 */
@Serializable
data class DishDTO(
    val name: String,

    val weight: UInt,
    val tdee: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,

    val id: Long? = null,
    val photoId: Long? = null, // Id файла в файловом хранилище.
    // Совпадение id в CH и в MinIO - ответственность заполняющего скрипта.
    // Этот сервис доступа к MinIO иметь не должен, только узнать нужный Id из CH и кинуть его дальше.
    val recipeId: Long? = null, // Как и с фото.
) {
    /**
     * Рассчитывает параметры блюда с [новым весом][newWeight]
     * @return Новый объект [DishDTO]
     */
    fun adjustWeight(newWeight: Double): DishDTO {
        val factor = newWeight / this.weight.toDouble()
        return this.copy(
            weight = newWeight.toUInt(),
            tdee = this.tdee * factor,
            protein = this.protein * factor,
            fat = this.fat * factor,
            carbs = this.carbs * factor
        )
    }
}

/**
 * Представление дневного рациона
 */
@Serializable
data class DailyDishSetDTO(
    val breakfast: DishDTO,
    val lunch: DishDTO,
    val dinner: DishDTO,
    val tdee: Double = breakfast.tdee + lunch.tdee + dinner.tdee,
    val protein: Double = breakfast.protein + lunch.protein + dinner.protein,
    val fat: Double = breakfast.fat + lunch.fat + dinner.fat,
    val carbs: Double = breakfast.carbs + lunch.carbs + dinner.carbs,
)
