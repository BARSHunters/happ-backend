package com.example.data

import kotlinx.serialization.Serializable
import serializers.LocalDateSerializer
import serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TokenValidationRequest(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID, val token: String
)

// message: "valid"/"invalid"
// contract: message: "valid" -> name: real name from JWT,
// else - the field will be ignored -> may be anything
@Serializable
data class TokenValidationResponse(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID, val message: String, val username: String
)

// DTO's

@Serializable
data class TokenDto(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    val token: String,
)

@Serializable
data class LoginDto(
    val username: String,
    val password: String
)

@Serializable
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

@Serializable
enum class Gender {
    MALE,
    FEMALE
}

@Serializable
enum class WeightDesire {
    LOSS,
    REMAIN,
    GAIN
}

