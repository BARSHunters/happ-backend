package model.request

import kotlinx.serialization.Serializable
import model.Gender
import model.WeightDesire
import utils.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class RegisterDto(
    val username: String,
    val password: String,
    val name: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate,
    val gender: Gender,
    val heightCm: Int,
    val weightKg: Float,
    val weightDesire: WeightDesire
)