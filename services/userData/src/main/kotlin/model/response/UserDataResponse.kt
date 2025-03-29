package model.response

import kotlinx.serialization.Serializable
import model.Gender
import model.WeightDesire
import utils.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class UserDataResponse(
    val username: String,
    val name: String,
    val age: Int,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate,
    val gender: Gender,
    val height: Int,
    val weight: Float,
    val weightDesire: WeightDesire
)
