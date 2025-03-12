package model

import java.util.UUID

data class Token(
    val id: UUID,
    val token: String,
    val tokenType: TokenType,
    var revoked: Boolean,
    var expired: Boolean,
    val user: User
)
