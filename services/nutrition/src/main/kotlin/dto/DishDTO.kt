package org.example.dto

data class DishDTO(
    val name: String,
    val tdee: UInt,
    val weight: UInt,
    val protein: UInt,
    val fat: UInt,
    val carbs: UInt,
    val id: Long? = null,
    val photoId: Long? = null, // Id файла в файловом хранилище.
    // Совпадение id в CH и в MinIO - ответственность заполняющего скрипта.
    // Этот сервис доступа к MinIO иметь не должен, только узнать нужный Id из CH и кинуть его дальше.
    val recipeId: Long? = null, // Как и с фото.
)

data class DailyDishSetDTO(
    val breakfast: DishDTO,
    val lunch: DishDTO,
    val dinner: DishDTO,
    val tdee: UInt = breakfast.tdee + lunch.tdee + dinner.tdee,
    val protein: UInt = breakfast.protein + lunch.protein + dinner.protein,
    val fat: UInt = breakfast.fat + lunch.fat + dinner.fat,
    val carbs: UInt = breakfast.carbs + lunch.carbs + dinner.carbs,
)
