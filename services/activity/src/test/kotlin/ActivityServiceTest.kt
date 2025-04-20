import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import utils.Gender

class ActivityServiceTest {
    private lateinit var activityService: ActivityService

    @BeforeEach
    fun setUp() {
        activityService = spyk(ActivityService())

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
        every { activityService.saveToDatabase(any()) } just Runs
    }

    @Test
    fun testParseWorkoutAndRemoveFromQueue() = runTest {
        val jsonWorkout = """
            {
                "duration": "01:30:00",
                "datetime": "2025-04-16 08:00:00",
                "name": "Утренняя пробежка",
                "intensityZones": [10, 15, 20, 10, 5],
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
        assertEquals(initialSize, finalSize)
    }

    @Test
    fun testParseWorkoutWithAllFields() {
        val jsonWorkout = """
            {
                "duration": "01:30:00",
                "datetime": "2025-04-16 08:00:00",
                "name": "Интервальная тренировка",
                "intensityZones": [5, 10, 15, 20, 10],
                "heartRates": [
                    {"timestamp": 1700000000, "heartRate": 120}
                ]
            }
        """.trimIndent()

        val testData = ExtendedTrainingData()
        activityService.parseWorkout(jsonWorkout, testData)

        assertEquals(5400, testData.trainingDuration)
        assertEquals("2025-04-16 08:00:00", testData.trainingDate)
        assertEquals("Интервальная тренировка", testData.trainingName)
        assertEquals(listOf(5, 10, 15, 20, 10), testData.intensityZones)
        assertEquals(listOf(1700000000L to 120), testData.heartRateList)
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
        val username = "testUser"
        val trainingDate = "2025-04-16 09:00:00"
        val jsonWorkout = """
        {
            "duration": "01:00:00",
            "datetime": "$trainingDate",
            "name": "Тестовая тренировка",
            "intensityZones": [5, 10, 15],
            "heartRates": [
                {"timestamp": 1700000000, "heartRate": 120}
            ]
        }
    """.trimIndent()

        // Мокаем сохранение в БД
        every { activityService.saveToDatabase(any()) } just Runs

        // Мокаем запрос к БД
        every {
            activityService.fetchFromDatabase(
                eq(username),
                eq("2025-04-16 00:00:00"),
                eq("2025-04-16 23:59:59")
            )
        } returns listOf(
            TrainingData(
                username = username,
                trainingName = "Тестовая тренировка",
                trainingDate = trainingDate,
                trainingDuration = 3600,
                avgHeartRate = 120.0,
                maxHeartRate = 140,
                caloriesBurned = 300.0,
                met = 5.0,
                intensityZones = listOf(5, 10, 15),
                recoveryTime = 86400
            )
        )

        activityService.processRequestAddTraining(username, jsonWorkout)

        val result = activityService.processRequestGetSomeTraining(
            username,
            "2025-04-16 00:00:00",
            "2025-04-16 23:59:59"
        )

        assertFalse(result.isEmpty())
        assertEquals(username, result[0].username)
        assertEquals(3600, result[0].trainingDuration)
        assertEquals("Тестовая тренировка", result[0].trainingName)
        assertEquals(listOf(5, 10, 15), result[0].intensityZones)
        assertEquals(86400, result[0].recoveryTime)
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

    @Test
    fun testFetchTrainingDataWithDateRange() = runTest {
        val username = "rangeTestUser"

        // Мокаем запрос к БД
        every {
            activityService.fetchFromDatabase(
                eq(username),
                any<String>(),
                any<String>()
            )
        } returns listOf(
            TrainingData(
                username = username,
                trainingName = "Утренняя пробежка",
                trainingDate = "2025-04-16 08:00:00",
                trainingDuration = 3600,
                avgHeartRate = 120.0,
                maxHeartRate = 140,
                caloriesBurned = 300.0,
                met = 5.0,
                intensityZones = listOf(10, 15, 20),
                recoveryTime = 86400 // 24 часа в секундах
            ),
            TrainingData(
                username = username,
                trainingName = "Вечерняя тренировка",
                trainingDate = "2025-04-16 18:00:00",
                trainingDuration = 1800,
                avgHeartRate = 110.0,
                maxHeartRate = 130,
                caloriesBurned = 200.0,
                met = 4.0,
                intensityZones = listOf(5, 10, 15),
                recoveryTime = 43200 // 12 часов в секундах
            )
        )

        val result = activityService.processRequestGetSomeTraining(
            username,
            "2025-04-16 00:00:00",
            "2025-04-16 23:59:59"
        )

        assertEquals(2, result.size)

        // Проверка первой тренировки
        assertEquals("Утренняя пробежка", result[0].trainingName)
        assertEquals(listOf(10, 15, 20), result[0].intensityZones)
        assertEquals(86400, result[0].recoveryTime)

        // Проверка второй тренировки
        assertEquals("Вечерняя тренировка", result[1].trainingName)
        assertEquals(listOf(5, 10, 15), result[1].intensityZones)
        assertEquals(43200, result[1].recoveryTime)
    }

    @Test
    fun testSingleHeartRateValue() = runTest {
        val jsonWorkout = """
        {
            "duration": "00:30:00",
            "datetime": "2025-04-16 10:00:00",
            "name": "Тест с одним пульсом",
            "intensityZones": [0, 0, 0, 0, 0],
            "heartRates": [
                {"timestamp": 1700000000, "heartRate": 120}
            ]
        }
    """.trimIndent()

        // Мокаем сохранение и получение данных
        every { activityService.saveToDatabase(any()) } just Runs
        every {
            activityService.fetchFromDatabase(eq("singleUser"), any(), any())
        } returns listOf(
            TrainingData(
                username = "singleUser",
                trainingName = "Тест с одним пульсом",
                trainingDate = "2025-04-16 10:00:00",
                trainingDuration = 1800,
                avgHeartRate = 120.0,
                maxHeartRate = 120,
                caloriesBurned = 17457.117, // Примерное значение для проверки
                met = 4.615,
                intensityZones = listOf(0, 0, 0, 0, 0),
                recoveryTime = 12345
            )
        )

        val result = activityService.processRequestAddTraining("singleUser", jsonWorkout)
        assertEquals("Workout processed and saved.", result)

        val trainingData = activityService.processRequestGetSomeTraining("singleUser").first()

        // Проверка метрик
        assertEquals(120, trainingData.maxHeartRate)
        assertEquals(120.0, trainingData.avgHeartRate, 0.01)
        assertEquals(1800, trainingData.trainingDuration) // 30 минут = 1800 секунд
        assertEquals(17457.117, trainingData.caloriesBurned, 0.001)
        assertEquals(4.615, trainingData.met, 0.001)
    }
}