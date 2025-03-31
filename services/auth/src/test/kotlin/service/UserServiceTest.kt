package service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import repository.TokenRepository
import repository.UserRepository
import kotlin.test.assertNull

class UserServiceTest {

    private lateinit var userService: UserService
    private val userRepository = mockk<UserRepository>()
    private val tokenRepository = mockk<TokenRepository>(relaxed = true) // relaxed позволяет избежать лишних заглушек

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, tokenRepository)
    }

    @Test
    fun `login should return null when user does not exist`() {
        every { userRepository.findUserByUsername(any()) } returns null

        val token = userService.login("unknownUser", "password123")

        assertNull(token)
        verify { userRepository.findUserByUsername("unknownUser") }
        verify(exactly = 0) { tokenRepository.save(any()) }
    }

}