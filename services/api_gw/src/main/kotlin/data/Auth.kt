package com.example.data

import java.util.*

data class TokenValidationRequest(val id: UUID, val token: String)

// message: "valid"/"invalid"
// contract: message: "valid" -> name: real name from JWT,
// else - the field will be ignored -> may be anything
data class TokenValidationResponse(val id: UUID, val message: String, val name: String)


// DTO's

/*
data class LoginDto(
    val username: String,
    val password: String
)

data class LoginResponse(
    val jwt: String
)

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

enum class Gender {
    MALE,
    FEMALE
}

enum class WeightDesire {
    LOSS,
    REMAIN,
    GAIN
}
*/

