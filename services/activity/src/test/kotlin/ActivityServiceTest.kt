import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ActivityServiceTest {
    private lateinit var activityService: ActivityService

    @BeforeEach
    fun setUp() {
        activityService = spyk(ActivityService()) // Используем spyk для частичного мокинга

        // Мокируем fetchUserData, чтобы он возвращал фиктивные данные
        coEvery { activityService.fetchUserData() } answers {
            activityService.weight = 70.0
            activityService.age = 30
            activityService.gender = "male"
        }

        // Мокируем вызов внешнего сервиса (делаем его пустым)
        every { activityService.sendTrainingDataToAchievementAndNotifyService() } just Runs
    }

    @Test
    fun testParseWorkout() =
        runTest {
            val jsonWorkout =
                """
                {
                    "duration": "01:30:00",
                    "heartRates": [
                        {"timestamp": 1700000000, "heartRate": 120},
                        {"timestamp": 1700000100, "heartRate": 130}
                    ]
                }
                """.trimIndent()

            activityService.processRequest("user1", jsonWorkout)
            assertEquals(5400, activityService.trainingDuration) // 1 час 30 минут = 5400 секунд
        }

    @Test
    fun testCalculateHeartRateMetrics() {
        val heartRates =
            listOf(
                1700000000L to 120,
                1700000100L to 130,
                1700000200L to 140,
            )
        activityService.heartRateList = heartRates
        activityService.calculateHeartRateMetrics()

        assertEquals(140, activityService.maxHeartRate)
        assertEquals(130.0, activityService.avgHeartRate)
    }

    @Test
    fun testCalculateCalories() {
        activityService.weight = 70.0
        activityService.age = 30
        activityService.gender = "male"
        activityService.avgHeartRate = 120.0
        activityService.trainingDuration = 3600

        activityService.calculateCalories()
        // Реальный ответ: 34914,23518...
        assertEquals(34914.235, activityService.caloriesBurned, 0.001)

        activityService.weight = 58.5
        activityService.age = 25
        activityService.gender = "female"
        activityService.avgHeartRate = 124.0
        activityService.trainingDuration = 2000

        activityService.calculateCalories()
        // Реальный ответ: 14107,09847...
        assertEquals(14107.098, activityService.caloriesBurned, 0.001)
    }

    @Test
    fun testCalculateMET() {
        activityService.avgHeartRate = 120.0
        activityService.age = 30
        activityService.calculateMET()
        // Реальный ответ: 4.61538...
        assertEquals(4.615, activityService.met, 0.001)
    }

    @Test
    fun testCalculateRecoveryTime() {
        activityService.met = 5.0
        activityService.avgHeartRate = 120.0
        activityService.maxHeartRate = 140
        activityService.calculateRecoveryTime()

        // Реальный ответ: 141942
        assertEquals(141942, activityService.recoveryTime)
    }

    @Test
    fun testSaveAndFetchFromDatabase() =
        runTest {
            val userId = "testUser"
            val trainingDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            // Сохраняем данные
            activityService.processRequest(
                userId,
                """
                {
                    "duration": "01:00:00",
                    "heartRates": [
                        {"timestamp": 1700000000, "heartRate": 120}
                    ]
                }
                """.trimIndent(),
                trainingDate,
            )

            // Получаем данные
            val result = activityService.processRequest(userId, trainingDate = trainingDate) as Map<*, *>
            assertEquals(userId, result["user_id"])
            assertEquals(3600, result["training_duration"]) // 1 час = 3600 секунд
        }

    @Test
    fun testInvalidWorkoutData() {
        val invalidJsonWorkout =
            """
            {
                "duration": "01:30",
                "heartRates": []
            }
            """.trimIndent()

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                activityService.processRequest("user1", invalidJsonWorkout)
            }
        }
    }

    @Test
    fun testInvalidUserData() {
        activityService.weight = -1.0
        activityService.age = 0
        activityService.gender = "unknown"

        assertThrows(IllegalArgumentException::class.java) {
            activityService.calculateCalories()
        }
    }
}
