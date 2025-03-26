package org.example.decider

import org.example.dto.DailyDishSetDTO
import org.example.dto.DishDTO
import org.example.model.User

object Decider {
    fun decide(user: User, wish: String): DailyDishSetDTO {
        // TODO
        return DailyDishSetDTO(
            DishDTO("", 0U, 0.0, 0.0, 0.0, 0.0),
            DishDTO("", 0U, 0.0, 0.0, 0.0, 0.0),
            DishDTO("", 0U, 0.0, 0.0, 0.0, 0.0),
        )
    }
}