package controller

import io.mockk.*
import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Gender
import model.WeightDesire
import model.request.LoginDto
import model.request.RegisterDto
import model.request.RequestWrapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import service.UserService
import java.time.LocalDate
import java.util.*

class AuthControllerTest {

    private lateinit var authController: AuthController
    private val userService: UserService = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        authController = AuthController(userService)
        mockkStatic(::sendEvent)
    }

    @Test
    fun `handleRegister should return token and send event when registration is successful`() {
        val request = RegisterDto(
            "newUser",
            "StrongPass123",
            "John Doe",
            LocalDate.of(1990, 1, 10),
            Gender.MALE,
            180,
            75.0f,
            WeightDesire.LOSS
        )
        val requestWrapper = RequestWrapper(UUID.fromString("1"), request)
        val requestJson = Json.encodeToString(requestWrapper)
        every { userService.register("newUser", "StrongPass123") } returns "mocked-token"
        authController.handleRegister(requestJson)
        verify { userService.register("newUser", "StrongPass123") }
        verify { sendEvent("registerResponse", any()) }
        verify { sendEvent("createUserDataRequest", any()) }
        unmockkStatic(::sendEvent)
    }

    @Test
    fun `handleLogin should return token when credentials are valid`() {
        val request = LoginDto("newUser", "StrongPass123")
        val requestWrapper = RequestWrapper(UUID.fromString("2"), request)
        val requestJson = Json.encodeToString(requestWrapper)
        every { userService.login("newUser", "StrongPass123") } returns "mocked-token"
        authController.handleLogin(requestJson)
        verify { userService.login("newUser", "StrongPass123") }
        verify { sendEvent("loginResponse", any()) }
    }

    @Test
    fun `handleLogin should return error when credentials are invalid`() {
        val request = LoginDto("newUser", "WrongPassword")
        val requestWrapper = RequestWrapper(UUID.fromString("3"), request)
        val requestJson = Json.encodeToString(requestWrapper)
        every { userService.login("newUser", "WrongPassword") } returns null
        authController.handleLogin(requestJson)
        verify { userService.login("newUser", "WrongPassword") }
    }
}
