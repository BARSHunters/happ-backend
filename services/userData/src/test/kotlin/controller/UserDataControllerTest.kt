package controller

import io.mockk.*
import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Gender
import model.UserData
import model.WeightDesire
import service.UserDataService
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class UserDataControllerTest {

    private val userDataService = mockk<UserDataService>()
    private val controller = UserDataController(userDataService)

    private val testUser = UserData(
        username = "testuser",
        name = "Test User",
        birthDate = LocalDate.of(1990, 1, 1),
        gender = Gender.MALE,
        heightCm = 180,
        weightKg = 75.0f,
        weightDesire = WeightDesire.REMAIN
    )

    @BeforeTest
    fun setUp() {
        clearAllMocks()
        mockkStatic(::sendEvent)
    }

    @Test
    fun `handleCreateUserData should send createWeightHistory event on success`() {
        val jsonRequest = Json.encodeToString(testUser)
        every { userDataService.createUserData(testUser) } returns true
        every { sendEvent(any(), any()) } just Runs
        controller.handleCreateUserData(jsonRequest)
        verify {
            sendEvent(
                "createWeightHistory",
                match { it.contains("\"username\":\"testuser\"") && it.contains("\"weightKg\":75.0") }
            )
        }
    }

    @Test
    fun `handleCreateUserData should send error on invalid json`() {
        every { sendEvent(any(), any()) } just Runs
        controller.handleCreateUserData("{invalid json}")
        verify {
            sendEvent(
                "error",
                match { it.contains("\"errorType\":\"BAD_REQUEST\"") && it.contains("Invalid JSON format") }
            )
        }
    }

    @Test
    fun `handleUpdateUserData should send createWeightHistory event on success`() {
        val jsonRequest = Json.encodeToString(testUser)
        every { userDataService.updateUserData(testUser) } returns true
        every { sendEvent(any(), any()) } just Runs
        controller.handleUpdateUserData(jsonRequest)
        verify {
            sendEvent(
                "createWeightHistory",
                match { it.contains("\"username\":\"testuser\"") && it.contains("\"weightKg\":75.0") }
            )
        }
    }

    @Test
    fun `receiveUserData should send sendUserData event when user exists`() {
        every { userDataService.getUserData("testuser") } returns testUser
        every { sendEvent(any(), any()) } just Runs
        controller.receiveUserData("testuser")
        verify {
            sendEvent(
                "sendUserData",
                match { it.contains("\"username\":\"testuser\"") }
            )
        }
    }

    @Test
    fun `receiveAge should send sendAge event with correct age`() {
        every { userDataService.getAge("testuser") } returns 35
        every { sendEvent(any(), any()) } just Runs
        controller.receiveAge("testuser")
        verify { sendEvent("sendAge", "35") }
    }

    @Test
    fun `receiveWeight should send error when user not found`() {
        every { userDataService.getWeightKg("testuser") } returns null
        every { sendEvent(any(), any()) } just Runs
        controller.receiveWeight("testuser")
        verify {
            sendEvent(
                "error",
                match { it.contains("\"errorType\":\"BAD_REQUEST\"") && it.contains("Can't get weight") }
            )
        }
    }
}