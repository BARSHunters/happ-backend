import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import utils.Gender
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ActivityServiceTest {
    private lateinit var activityService: ActivityService

    @BeforeEach
    fun setUp() {
        activityService = spyk(ActivityService())

        // Мокаем fetchUserData для обновления объекта ExtendedTrainingData
        coEvery {
            activityService.fetchUserData(any())
        } answers {
            val request = it.invocation.args[0] as ExtendedTrainingData
            request.weight = 70.0F
            request.age = 30
            request.gender = Gender.MALE
            request.userDataReceived.complete(Unit)
        }

        every { activityService.sendTrainingDataToAchievementAndNotifyService(any()) } just Runs
    }

    @Test
    fun testParseWorkoutAndRemoveFromQueue() = runTest {
        val jsonWorkout = """
            {
                "duration": "01:30:00",
                "heartRates": [
                    {"timestamp": 1700000000, "heartRate": 120},
                    {"timestamp": 1700000100, "heartRate": 130}
                ]
            }
        """.trimIndent()

        val initialSize = activityService.requestTrainingDataList.size
        val result = activityService.processRequestAddTraining("user1", jsonWorkout)
        val finalSize = activityService.requestTrainingDataList.size

        assertEquals("Workout processed and saved.", result)
        assertEquals(initialSize, finalSize) // объект должен быть удалён после обработки
    }

    @Test
    fun testCalculateHeartRateMetrics() {
        val testData = ExtendedTrainingData(
            heartRateList = listOf(
                1700000000L to 120,
                1700000100L to 130,
                1700000200L to 140,
            )
        )

        activityService.calculateHeartRateMetrics(testData)
        assertEquals(140, testData.maxHeartRate)
        assertEquals(130.0, testData.avgHeartRate, 0.1)
    }

    @Test
    fun testCalculateCalories() {
        val testData = ExtendedTrainingData(
            weight = 70.0F,
            age = 30,
            gender = Gender.MALE,
            avgHeartRate = 120.0,
            trainingDuration = 3600
        )

        activityService.calculateCalories(testData)
        assertEquals(34914.235, testData.caloriesBurned, 0.001)
    }

    @Test
    fun testCalculateMET() {
        val testData = ExtendedTrainingData(
            avgHeartRate = 120.0,
            age = 30
        )
        activityService.calculateMET(testData)
        assertEquals(4.615, testData.met, 0.001)
    }

    @Test
    fun testCalculateRecoveryTime() {
        val testData = ExtendedTrainingData(
            met = 5.0,
            avgHeartRate = 120.0,
            maxHeartRate = 140
        )
        activityService.calculateRecoveryTime(testData)
        assertEquals(141942, testData.recoveryTime)
    }

    @Test
    fun testSaveAndFetchFromDatabase() = runTest {
        val userId = "testUser"
        val trainingDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val jsonWorkout = """
            {
                "duration": "01:00:00",
                "heartRates": [
                    {"timestamp": 1700000000, "heartRate": 120}
                ]
            }
        """.trimIndent()

        activityService.processRequestAddTraining(userId, jsonWorkout, trainingDate)

        val result = activityService.processRequestGetSomeTraining(userId, trainingDate)
        assertEquals(userId, result.userId)
        assertEquals(3600, result.trainingDuration)
    }

    @Test
    fun testInvalidWorkoutData() {
        val invalidJsonWorkout = """
            {
                "duration": "01:30",
                "heartRates": []
            }
        """.trimIndent()

        assertThrows<RuntimeException> {
            runBlocking {
                activityService.processRequestAddTraining("user1", invalidJsonWorkout)
            }
        }
    }

    @Test
    fun testInvalidUserData() {
        val testData = ExtendedTrainingData(
            weight = -1.0F,
            age = 0
        )
        assertThrows<IllegalArgumentException> {
            activityService.calculateCalories(testData)
        }
    }
}
