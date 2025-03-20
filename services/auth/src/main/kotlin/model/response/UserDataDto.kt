package model.response

import kotlinx.serialization.Serializable
import model.Gender
import model.WeightDesire
import utils.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class UserDataDto(
    val username: String,
    val name: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate,
    val gender: Gender,
    val heightCm: Int,
    val weightKg: Float,
    val weightDesire: WeightDesire
)
