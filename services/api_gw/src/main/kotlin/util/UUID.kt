package com.example.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import serializers.UUIDSerializer
import java.util.*

@Serializable
private data class UUIDResponse(@Serializable(with = UUIDSerializer::class) val uuid: UUID)

private val json = Json { ignoreUnknownKeys = true }

fun uuidEquals(uuid: UUID): (String) -> Boolean = { msg ->
    runCatching { json.decodeFromString<UUIDResponse>(msg).uuid == uuid }
        .onFailure { System.err.println("cause by this message: $msg") }.getOrThrow()
}

@Serializable
data class UUIDWrapper<T>(@Serializable(with = UUIDSerializer::class) val uuid: UUID, val dto: T)
