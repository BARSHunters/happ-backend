import io.mockk.*
import keydb.sendEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.decider.Decider
import org.example.decider.Wish
import org.example.dto.*
import org.example.model.Gender
import org.example.model.User
import org.example.service.HistoryService
import org.example.service.RationCacheService
import java.time.LocalDate
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class RationUpdateTests {
    private val rationCacheService = mockk<RationCacheService>()
    private val historyService = mockk<HistoryService>()
    private val decider = mockk<Decider>()

    private val rationController = org.example.api.RationController

    @BeforeTest
    fun setUp() {
        clearAllMocks()
        mockkStatic(::sendEvent)
    }

    @Test
    fun `init ration update request test`() {
        val testLogin = "test_user"
        val request = UpdateRationRequestDTO(UUID.randomUUID(), testLogin, MealType.BREAKFAST)
        val nextReq = RationRequestDTO(request.id, request.login)

        every { rationCacheService.initQuery(nextReq) } just Runs

        val requestJSON = Json.encodeToString(request)
        rationController.requestTodayRation(requestJSON)

        verify {
            rationCacheService.initQuery(nextReq)
            sendEvent("weight_history:request:WeightControlWish", match { it == Json.encodeToString(nextReq) })
        }
    }

    // `wish response handle test` stays same as in RationTests
    // `activity index response handle test` stays same as in RationTests

    @Test
    fun `ration update response test`() {
        // отличие в вызове прочитанном из кеша MealType.BREAKFAST swap

        val testLogin = "test_user"
        val request = UserDataResponseDTO(
            UUID.randomUUID(),
            UserDTO(
                testLogin,
                testLogin,
                LocalDate.now(),
                Gender.MALE,
                171,
                71.0f,
                Wish.REMAIN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
            )
        )
        val user = User(request.dto, 1.2f)
        val response = DailyDishSetDTO(
            DishDTO("dish", 10u, 10.0, 10.0, 10.0, 10.0),
            DishDTO("dish", 10u, 10.0, 10.0, 10.0, 10.0),
            DishDTO("dish", 10u, 10.0, 10.0, 10.0, 10.0),
            10.0, 10.0, 10.0, 10.0
        )

        every { rationCacheService.getByQueryId(request.id) } returns RationCacheDTO(
            request.id, testLogin, Wish.REMAIN, MealType.BREAKFAST, 1.2f // тут
        )
        every { decider.swap(user, Wish.REMAIN, MealType.BREAKFAST) } returns response // тут
        every { rationCacheService.clearQuery(request.id) } just Runs
        every { historyService.addHistory(testLogin, response) } just Runs

        val requestJSON = Json.encodeToString(request)
        rationController.afterFetchFromUserDataService(requestJSON)

        verify {
            decider.swap(user, Wish.REMAIN, MealType.BREAKFAST) // тут
            rationCacheService.clearQuery(request.id)
            historyService.addHistory(testLogin, response)
            sendEvent("weight_history:request:WeightControlWish", match { it == Json.encodeToString(response) })
        }
    }
}