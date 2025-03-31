package model

import kotlinx.serialization.Serializable
import utils.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class UserData(
    val username: String,
    val name: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate,
    val age: Int,
    val gender: Gender,
    val height: Int,
    val weight: Float,
    val weightDesire: WeightDesire
)

@Suppress("unused")
enum class Gender {
    MALE,
    FEMALE
}

@Suppress("unused")
enum class WeightDesire {
    LOSS,
    REMAIN,
    GAIN
}
