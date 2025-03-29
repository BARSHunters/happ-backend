package org.example.dto

import java.util.*

/**
 * Представление ошибки: UUID запроса + текст ошибки
 */
data class ErrorDTO(
    val id: UUID,
    val msg: String
)