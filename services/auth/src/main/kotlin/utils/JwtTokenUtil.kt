package utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import java.util.*

object JwtTokenUtil {
    private val dotenv = dotenv { directory = "./services/auth" }
    private val secretKey = dotenv["JWT_SECRET"].toString()

    private val jwtExpiration = dotenv["JWT_EXPIRATION"].toLong()

    private val algorithm = Algorithm.HMAC256(secretKey)

    fun createToken(username: String): String {
        return JWT.create()
            .withSubject(username)
            .withIssuedAt(Date(System.currentTimeMillis()))
            .withExpiresAt(Date(System.currentTimeMillis() + jwtExpiration))
            .sign(algorithm)
    }

    fun verifyToken(token: String): Boolean = runCatching {
        JWT.require(algorithm).build().verify(token)
    }.isSuccess

}