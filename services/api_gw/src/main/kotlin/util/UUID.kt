package com.example.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
private data class UUIDResponse(@Serializable(with = UUIDSerializer::class) val uuid: UUID)

private val json = Json { ignoreUnknownKeys = true }

fun uuidEquals(uuid: UUID) : (String) -> Boolean = {
    json.decodeFromString<UUIDResponse>(it).uuid == uuid
}

@Serializable
data class UUIDWrapper<T>(@Serializable(with = UUIDSerializer::class) val uuid: UUID, val dto: T)
