package com.example.util

import kotlinx.serialization.json.Json
import java.util.*

private data class UUIDResponse(val uuid: UUID)

private val json = Json { ignoreUnknownKeys = true }

fun uuidEquals(uuid: UUID) : (String) -> Boolean = {
    json.decodeFromString<UUIDResponse>(it).uuid == uuid
}

data class UUIDWrapper<T>(val uuid: UUID, val dto: T)
