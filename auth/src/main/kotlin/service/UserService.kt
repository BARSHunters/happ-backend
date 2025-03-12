package service

import model.Token
import model.TokenType
import model.User
import repository.TokenRepository
import repository.UserRepository
import utils.JwtTokenUtil
import utils.PasswordHasher
import java.util.*

class UserService(private val userRepository: UserRepository, private val tokenRepository: TokenRepository) {


    // TODO добавить хэширование пароля
    fun register(username: String, password: String): String? {
        val existingUser = userRepository.findUserByUsername(username) ?: return null
        val hashedPassword = PasswordHasher.hash(password)
        if (userRepository.createUser(username, hashedPassword)) {
            val newToken = Token(
                UUID.randomUUID(),
                JwtTokenUtil.createToken(username),
                TokenType.BEARER,
                revoked = false,
                expired = false,
                user = existingUser
            )
            tokenRepository.save(newToken)
            return newToken.token
        }
        return null
    }


    // TODO добавить аутентификацию пользователя
    fun login(username: String, password: String): String? {
        val existingUser = userRepository.findUserByUsername(username)
        val hashedPassword = PasswordHasher.hash(existingUser.password)
        if (existingUser != null && PasswordHasher.verify(password, hashedPassword)) {
            revokeAllUserTokens(existingUser)
            val newToken = Token(
                UUID.randomUUID(),
                JwtTokenUtil.createToken(username),
                TokenType.BEARER,
                revoked = false,
                expired = false,
                user = existingUser
            )
            tokenRepository.save(newToken)
            return newToken.token
        }
        return null
    }

    private fun revokeAllUserTokens(user: User) {
        val validUserTokens = tokenRepository.findAllValidTokenByUser(user.username)
        if (validUserTokens?.isEmpty() == true)
            return
        validUserTokens?.forEach {
            it.expired = true
            it.revoked = true
        }
//        tokenRepository.saveAll(validUserTokens!!)
    }
}