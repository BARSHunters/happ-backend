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

class RationTests {
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
    fun `init ration request test`() {
        val testLogin = "test_user"
        val request = RationRequestDTO(UUID.randomUUID(), testLogin)

        every { rationCacheService.initQuery(request) } just Runs

        val requestJSON = Json.encodeToString(request)
        rationController.requestTodayRation(requestJSON)

        verify {
            rationCacheService.initQuery(request)
            sendEvent("weight_history:request:WeightControlWish", match { it == requestJSON })
        }
    }

    @Test
    fun `wish response handle test`() {
        val testLogin = "test_user"
        val request = WishResponseDTO(UUID.randomUUID(), Wish.REMAIN)

        every { rationCacheService.getByQueryId(request.id) } returns RationCacheDTO(
            request.id, testLogin, null, null, null
        )
        every { rationCacheService.saveWish(request.id, request.wish) } just Runs

        val requestJSON = Json.encodeToString(request)
        rationController.afterFetchFromWeightHistoryService(requestJSON)

        verify {
            rationCacheService.saveWish(request.id, request.wish)
            sendEvent(
                "activity:request:ActivityIndex",
                match { it == Json.encodeToString(RationRequestDTO(request.id, testLogin)) })
        }
    }

    @Test
    fun `activity index response handle test`() {
        val testLogin = "test_user"
        val request = ActivityResponseDTO(UUID.randomUUID(), 1.2f)

        every { rationCacheService.getByQueryId(request.id) } returns RationCacheDTO(
            request.id, testLogin, Wish.REMAIN, null, null
        )
        every { rationCacheService.saveActivity(request.id, request.activityIndex) } just Runs

        val requestJSON = Json.encodeToString(request)
        rationController.afterFetchFromActivityService(requestJSON)

        verify {
            rationCacheService.saveActivity(request.id, request.activityIndex)
            sendEvent(
                "user_data:request:UserData",
                match { it == Json.encodeToString(UserDataRequestDTO(request.id, testLogin)) })
        }
    }

    @Test
    fun `ration response test`() {
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
            request.id, testLogin, Wish.REMAIN, null, 1.2f
        )
        every { decider.decide(user, Wish.REMAIN) } returns response
        every { rationCacheService.clearQuery(request.id) } just Runs
        every { historyService.addHistory(testLogin, response) } just Runs

        val requestJSON = Json.encodeToString(request)
        rationController.afterFetchFromUserDataService(requestJSON)

        verify {
            decider.decide(user, Wish.REMAIN)
            rationCacheService.clearQuery(request.id)
            historyService.addHistory(testLogin, response)
            sendEvent("weight_history:request:WeightControlWish", match { it == Json.encodeToString(response) })
        }
    }
}