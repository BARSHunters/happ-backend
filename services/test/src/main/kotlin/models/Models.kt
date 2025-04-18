package com.example.test.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDto(
    val username: String,
    val password: String,
    val name: String,
    val birthDate: String,
    val gender: String,
    val heightCm: Int,
    val weightKg: Float,
    val weightDesire: String
)

@Serializable
data class LoginDto(
    val username: String,
    val password: String
)

@Serializable
data class ActivityDTO(
    val duration: String, // format hh:mm:ss
    val heartRates: List<HeartRate>
)

@Serializable
data class HeartRate(
    val timestamp: Long,
    val heartRate: Int
)

@Serializable
data class UserDataDto(
    val username: String,
    val name: String,
    val birthDate: String,
    val gender: String,
    val heightCm: Int,
    val weightKg: Float,
    val weightDesire: String,
)