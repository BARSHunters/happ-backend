package model

import kotlinx.serialization.Serializable
import utils.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class UserData(
    val username: String,
    val name: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDay: LocalDate,
    val gender: Gender,
    val heightCm: Int,
    val weightKg: Float,
    val weightDesire: WeightDesire
)