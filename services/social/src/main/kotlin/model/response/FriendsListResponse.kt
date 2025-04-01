package model.response

import kotlinx.serialization.Serializable

@Serializable
data class FriendsListResponse(
    val friends: List<String>,
    val friendsCount: Int
) 