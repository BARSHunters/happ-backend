import io.mockk.*
import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.dto.*
import org.example.service.HistoryService
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.*
import kotlin.test.BeforeTest

class HistoryTests {
    private val historyService = mockk<HistoryService>()
    private val historyController = org.example.api.HistoryController

    @BeforeTest
    fun setUp() {
        clearAllMocks()
        mockkStatic(::sendEvent)
    }

    companion object {
        @JvmStatic
        fun `2 days test variants`(): List<Pair<Map<String, HistoryRow>, String>> {
            return listOf(
                Pair(
                    mapOf(
                        "2024-01-01" to HistoryRow(10.0, 10.0, 10.0, 10.0),
                        "2024-01-02" to HistoryRow(10.0, 10.0, 10.0, 10.0)
                    ),
                    "{" +
                            "\"2024-01-01\":{\"calories\":10.0,\"protein\":10.0,\"fat\":10.0,\"carbs\":10.0}," +
                            "\"2024-01-02\":{\"calories\":10.0,\"protein\":10.0,\"fat\":10.0,\"carbs\":10.0}" +
                            "}"
                ),
                Pair(
                    mapOf(
                        "2024-01-01" to HistoryRow(10.0, 10.0, 10.0, 10.0),
                    ),
                    "{\"2024-01-01\":{\"calories\":10.0,\"protein\":10.0,\"fat\":10.0,\"carbs\":10.0}}"
                ),
                Pair(
                    mapOf(),
                    ""
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("2 days test variants")
    fun `2 days test`(
        data: Pair<Map<String, HistoryRow>, String>
    ) {
        val (mockedData: Map<String, HistoryRow>, resultJSON: String) = data

        val testLogin = "test_user"
        val testDays = 2
        val request = Json.encodeToString(HistoryRequestDTO(UUID.randomUUID(), testLogin, testDays))

        every { historyService.getHistoryTDEEForUser(testLogin, testDays) } returns mockedData

        historyController.getRationHistory(request)

        verify {
            sendEvent("nutrition:response:CPFC", match { it == resultJSON })
        }
    }

    @Test
    fun `negative amount of days test`() {
        val testLogin = "test_user"
        val testDays = -2
        val request = Json.encodeToString(HistoryRequestDTO(UUID.randomUUID(), testLogin, testDays))

        assertThrows(IllegalArgumentException::class.java) { historyController.getRationHistory(request) }
    }

    @Test
    fun `zero amount of days test`() {
        val testLogin = "test_user"
        val testDays = 0
        val request = Json.encodeToString(HistoryRequestDTO(UUID.randomUUID(), testLogin, testDays))

        assertThrows(IllegalArgumentException::class.java) { historyController.getRationHistory(request) }
    }

    @Test
    fun `get ration by date test`() {
        val testLogin = "test_user"
        val date = LocalDate.now()
        val request = HistoryRequestRationByDateDTO(UUID.randomUUID(), testLogin, date)

        val result = DailyDishSetDTO(
            DishDTO("dish", 10u, 10.0, 10.0, 10.0, 10.0),
            DishDTO("dish", 10u, 10.0, 10.0, 10.0, 10.0),
            DishDTO("dish", 10u, 10.0, 10.0, 10.0, 10.0),
            10.0, 10.0, 10.0, 10.0
        )

        every { historyService.getFromHistoryRationByDate(testLogin, date) } returns result

        val requestJSON = Json.encodeToString(request)
        historyController.getRationByDate(requestJSON)

        verify {
            sendEvent(
                "nutrition:response:ration_by_date",
                match { it == Json.encodeToString(RationResponseDTO(request.id, result)) })
        }
    }
}