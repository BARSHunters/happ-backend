package service

import model.User
import repository.UserRepository

class UserService(private val userRepository: UserRepository) {


    // TODO добавить хэширование пароля
    fun register(username: String, password: String): String?{
        val existingUser = userRepository.findUserByUsername(username)
        if (existingUser != null) return null

        val hashedPassword = "password"
        return if (userRepository.createUser(username, hashedPassword)){
            "jwt"
        } else null
    }


    // TODO добавить аутентификацию пользователя
    fun login(username: String, password: String): String?{
        return "jwt"
    }
}