package org.example.dto

import java.util.*

data class ErrorDTO(
    val queryId: UUID,
    val msg: String
)