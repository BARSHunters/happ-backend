package model.response

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val username: String,
    val name: String,
)
