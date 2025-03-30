package model

import java.time.LocalDateTime
import java.util.*

data class Token(
    val id: UUID,
    val token: String,
    val tokenType: TokenType,
    var revoked: Boolean,
    val expiredAt: LocalDateTime,
    val userUsername: String,
)
