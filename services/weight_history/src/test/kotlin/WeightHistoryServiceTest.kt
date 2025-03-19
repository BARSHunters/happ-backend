import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WeightHistoryServiceTest {
    private lateinit var weightHistoryService: WeightHistoryService

    @BeforeEach
    fun setUp() {
        // Создаем мок для WeightHistoryService
        weightHistoryService = spyk(WeightHistoryService())

        // Мокируем функции, которые обращаются к внешним сервисам или базе данных
        mockkStatic(::sendEvent)
        every { sendEvent(any(), any()) } returns Unit

        // Мокируем функции, которые работают с базой данных
        every { weightHistoryService.fetchWeightHistoryFromDB(any()) } returns emptyList()
        every { weightHistoryService.fetchWeightControlWishFromDB(any()) } returns "keep"
        every { weightHistoryService.saveWeightToDB(any(), any(), any()) } returns Unit
        every { weightHistoryService.saveWeightControlWishToDB(any(), any()) } returns Unit
    }

    @Test
    fun testProcessRequest() {
        // Мокируем данные, которые возвращаются из внешних сервисов
        every { weightHistoryService.fetchActivityData(any()) } answers {
            weightHistoryService.handleActivityResponse(
                Json.encodeToString(
                    ActivityResponse(
                        userId = "user1",
                        activities =
                            listOf(
                                ActivityRecord("2023-10-01", 500.0),
                                ActivityRecord("2023-10-02", 600.0),
                            ),
                    ),
                ),
            )
        }

        every { weightHistoryService.fetchUserData(any()) } answers {
            weightHistoryService.handleUserDataResponse(
                Json.encodeToString(
                    UserDataResponse(
                        userId = "user1",
                        weight = 70.0,
                        age = 30,
                        gender = "male",
                    ),
                ),
            )
        }

        every { weightHistoryService.fetchNutritionData(any()) } answers {
            weightHistoryService.handleNutritionResponse(
                Json.encodeToString(
                    NutritionResponse(
                        userId = "user1",
                        nutritionData =
                            listOf(
                                "2023-10-01" to mapOf("calories" to 2000.0),
                                "2023-10-02" to mapOf("calories" to 2200.0),
                            ),
                    ),
                ),
            )
        }

        // Вызываем метод processRequest
        val result = weightHistoryService.processRequest("user1", "keep")

        // Проверяем, что результат содержит ожидаемые данные
        assertTrue(result.containsKey("user_id"))
        assertTrue(result.containsKey("weight_history"))
        assertEquals("user1", result["user_id"])
    }

    @Test
    fun testSaveWeightToDB() {
        // Мокируем функцию saveWeightToDB
        every { weightHistoryService.saveWeightToDB(any(), any(), any()) } returns Unit

        // Вызываем метод saveWeightToDB
        weightHistoryService.saveWeightToDB(
            "user1",
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            70.0,
        )

        // Проверяем, что метод был вызван
        verify { weightHistoryService.saveWeightToDB("user1", any(), 70.0) }
    }

    @Test
    fun testFetchWeightHistoryFromDB() {
        // Мокируем функцию fetchWeightHistoryFromDB
        every { weightHistoryService.fetchWeightHistoryFromDB(any()) } returns
            listOf(
                WeightHistoryEntry("2023-10-01T12:00:00", 70.0),
                WeightHistoryEntry("2023-10-02T12:00:00", 69.5),
            )

        // Вызываем метод fetchWeightHistoryFromDB
        val history = weightHistoryService.fetchWeightHistoryFromDB("user1")

        // Проверяем, что данные возвращаются корректно
        assertEquals(2, history.size)
        assertEquals(70.0, history[0].weight)
        assertEquals(69.5, history[1].weight)
    }

    @Test
    fun testValidateWeight() {
        // Проверяем, что корректный вес проходит валидацию
        assertDoesNotThrow { weightHistoryService.validateWeight(70.0) }

        // Проверяем, что некорректный вес вызывает исключение
        assertThrows(IllegalArgumentException::class.java) {
            weightHistoryService.validateWeight(-1.0)
        }
    }

    @Test
    fun testValidateDateTime() {
        // Проверяем, что корректная дата проходит валидацию
        val validDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        assertDoesNotThrow { weightHistoryService.validateDateTime(validDateTime) }

        // Проверяем, что некорректная дата вызывает исключение
        assertThrows(IllegalArgumentException::class.java) {
            weightHistoryService.validateDateTime("invalid-date")
        }
    }
}
