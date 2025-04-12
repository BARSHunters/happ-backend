package com.example.data

import kotlinx.serialization.Serializable
import serializers.LocalDateSerializer
import serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class UserDataRequest(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID, val username: String
)

@Serializable
data class UserDataDTO(
    val username: String,
    val name: String,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate,
    val gender: Gender,
    val heightCm: Int,
    val weightKg: Float,
    val weightDesire: WeightDesire
)
