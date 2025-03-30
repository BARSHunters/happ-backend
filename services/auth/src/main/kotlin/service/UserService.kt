package service

import io.github.cdimascio.dotenv.dotenv
import model.Token
import model.TokenType
import repository.TokenRepository
import repository.UserRepository
import utils.JwtTokenUtil
import utils.PasswordHasher
import java.time.LocalDateTime
import java.util.*

class UserService(private val userRepository: UserRepository, private val tokenRepository: TokenRepository) {

    private val dotenv = dotenv()
    private val jwtExpirationSeconds = dotenv["JWT_EXPIRATION"].toLong() / 100
    fun register(username: String, password: String): String? {
        if (userRepository.findUserByUsername(username) != null) {
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

    fun validateJwtToken(token: String): String? {
        if (!JwtTokenUtil.verifyToken(token)) {
            return null
        }
        val dbToken = tokenRepository.findByToken(token) ?: return null
        if (!(dbToken.revoked || dbToken.expiredAt.isBefore(LocalDateTime.now()))) {
            return dbToken.userUsername
        }
        return null
    }

    fun revokeJwtToken(token: String): Boolean {
        if (!JwtTokenUtil.verifyToken(token)) {
            return false
        }
        val dbToken = tokenRepository.findByToken(token) ?: return false
        dbToken.revoked = true
        return tokenRepository.revokeToken(dbToken)
    }
}