package com.example.data

import kotlinx.serialization.Serializable
import serializers.UUIDSerializer
import java.util.*

@Serializable
data class UserDataRequest(
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID, val username: String
)
