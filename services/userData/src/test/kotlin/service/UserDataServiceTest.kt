package service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import model.Gender
import model.UserData
import model.WeightDesire
import repository.UserRepository
import java.time.LocalDate
import kotlin.test.*

class UserDataServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val service = UserDataService(userRepository)

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
    }

    @Test
    fun `createUserData should return true when user doesn't exist`() {
        every { userRepository.findByUsername("testuser") } returns null
        every { userRepository.createUser(testUser) } returns true

        val result = service.createUserData(testUser)

        assertTrue(result)
        verify {
            userRepository.findByUsername("testuser")
            userRepository.createUser(testUser)
        }
    }

    @Test
    fun `createUserData should return false when user exists`() {
        every { userRepository.findByUsername("testuser") } returns testUser

        val result = service.createUserData(testUser)

        assertFalse(result)
        verify { userRepository.findByUsername("testuser") }
        verify(exactly = 0) { userRepository.createUser(any()) }
    }

    @Test
    fun `updateUserData should return true when fields updated`() {
        val updatedUser = testUser.copy(name = "Updated User")
        every { userRepository.findByUsername("testuser") } returns testUser
        every { userRepository.updateUserFields("testuser", mapOf("name" to "Updated User")) } returns true

        val result = service.updateUserData(updatedUser)

        assertTrue(result)
        verify {
            userRepository.findByUsername("testuser")
            userRepository.updateUserFields("testuser", mapOf("name" to "Updated User"))
        }
    }

    @Test
    fun `updateUserData should return false when user not found`() {
        every { userRepository.findByUsername("testuser") } returns null

        val result = service.updateUserData(testUser)

        assertFalse(result)
        verify { userRepository.findByUsername("testuser") }
        verify(exactly = 0) { userRepository.updateUserFields(any(), any()) }
    }

    @Test
    fun `getUserData should return user when found`() {
        every { userRepository.findByUsername("testuser") } returns testUser

        val result = service.getUserData("testuser")

        assertEquals(testUser, result)
        verify { userRepository.findByUsername("testuser") }
    }

    @Test
    fun `getAge should calculate age correctly`() {
        every { userRepository.findByUsername("testuser") } returns testUser

        val result = service.getAge("testuser")

        assertEquals(LocalDate.now().year - 1990, result)
        verify { userRepository.findByUsername("testuser") }
    }

    @Test
    fun `getWeightKg should return weight when user exists`() {
        every { userRepository.findByUsername("testuser") } returns testUser

        val result = service.getWeightKg("testuser")

        assertEquals(75.0f, result)
        verify { userRepository.findByUsername("testuser") }
    }
}