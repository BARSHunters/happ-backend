package model

data class Profile(
    val username: String,
    var name: String,
    var friendCount: Int,
    var age: Int,
    var height: Int,
    var weight: Float
)
