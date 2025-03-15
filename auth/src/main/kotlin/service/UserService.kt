package service

import io.github.cdimascio.dotenv.dotenv
import model.Token
import model.TokenType
import model.User
import repository.TokenRepository
import repository.UserRepository
import utils.JwtTokenUtil
import utils.PasswordHasher
import java.time.LocalDateTime
import java.util.*

class UserService(private val userRepository: UserRepository, private val tokenRepository: TokenRepository) {

    private val dotenv = dotenv()
    private val jwtExpirationSeconds = dotenv["JWT_EXPIRATION"].toLong()/100

    // TODO добавить хэширование пароля
    fun register(username: String, password: String): String? {
         if (userRepository.findUserByUsername(username) != null){
             return null
         }
        val hashedPassword = PasswordHasher.hash(password)
        if (userRepository.createUser(username, hashedPassword)) {
            val newToken = Token(
                UUID.randomUUID(),
                JwtTokenUtil.createToken(username),
                TokenType.BEARER,
                revoked = false,
                expiredAt = LocalDateTime.now().plusSeconds(jwtExpirationSeconds),
                userUsername = username
            )
            tokenRepository.save(newToken)
            return newToken.token
        }
        return null
    }


    // TODO добавить аутентификацию пользователя
    fun login(username: String, password: String): String? {
        val existingUser = userRepository.findUserByUsername(username)
        if (existingUser != null && PasswordHasher.verify(password, existingUser.password)) {
            val newToken = Token(
                UUID.randomUUID(),
                JwtTokenUtil.createToken(username),
                TokenType.BEARER,
                revoked = false,
                expiredAt = LocalDateTime.now().plusSeconds(jwtExpirationSeconds),
                userUsername = username
            )
            tokenRepository.save(newToken)
            return newToken.token
        }
        return null
    }

}