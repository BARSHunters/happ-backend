package model.response

import model.Gender
import model.WeightDesire
import java.time.LocalDate

data class UserDataDto(
    val username: String,
    val name: String,
    val birthDate: LocalDate,
    val gender: Gender,
    val heightCm: Int,
    val weightKg: Float,
    val weightDesire: WeightDesire
    )
