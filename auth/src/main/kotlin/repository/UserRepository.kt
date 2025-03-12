package repository

import model.User

class UserRepository {
    // TODO добавить логику добавления пользователя в бд
    fun createUser(username: String, password: String): Boolean{
        return true
    }

    // TODO добавить логику нахождения пользователя в бд
    fun findUserByUsername(username: String): User {
        return User(username = "username", password = "password")
    }
}