package org.example.dto

import kotlinx.serialization.Serializable
import org.example.utils.UUIDSerializer
import java.util.*

/**
 * Представление ошибки: UUID запроса + текст ошибки
 */
@Serializable
data class ErrorDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val msg: String
)