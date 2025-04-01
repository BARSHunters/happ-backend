import keydb.runServiceListener
import keydb.sendEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import utils.UUIDSerializer
import utils.LocalDateSerializer
import utils.Gender
import utils.WeightDesire

/**
 * Оболочка для поддержки запросов с UUID (Слава)
 */
@Serializable
data class RequestWrapper<T>(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val dto: T
)

/**
 * Оболочка для поддержки ответов с UUID (Слава)
 */
@Serializable
data class ResponseWrapper<T>(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val dto: T
)

/**
 * Оболочка для поддержки get-запросов по username (Слава)
 */
@Serializable
data class GetterDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val username: String,
)

/**
 * Оболочка для общения с API Gateway (Кирилл А.)
 */
data class UUIDWrapper<T>(val uuid: UUID, val dto: T)

/**
 * Ответ от сервиса активности.
 * @property username Идентификатор пользователя.
 * @property activities Список записей активности.
 */
@Serializable
data class ActivityResponse(
    val username: String,
    val activities: List<ActivityRecord>,
)

/**
 * Запись активности.
 * @property date Дата активности.
 * @property calories Количество сожжённых калорий.
 */
@Serializable
data class ActivityRecord(
    val date: String,
    val calories: Double,
)

/**
 * Запись истории веса.
 * @property dateTime Дата и время записи.
 * @property weight Вес пользователя.
 */
@Serializable
data class WeightHistoryEntry(val dateTime: String, val weight: Float)

/**
 * Ответ от сервиса пользовательских данных.
 * @property username Идентификатор пользователя.
 * @property weight Вес пользователя.
 * @property age Возраст пользователя.
 * @property gender Пол пользователя.
 * Остальные - не используются
 */
@Serializable
data class UserDataResponse(
    val username: String,
    val name: String,
    val age: Int,
    @Serializable(with = LocalDateSerializer::class)
    val birthDate: LocalDate,
    val gender: Gender,
    val height: Int,
    val weight: Float,
    val weightDesire: WeightDesire
)

/**
 * Запрос от userData с указанием нового веса
 * @property username Идентификатор пользователя.
 * @property weightKg Вес пользователя.
 */
@Serializable
data class NewWeightResponse(
    val username: String,
    val weightKg: Float
)

/**
 * Представление запроса на значение пожелания по весу от Nutrition
 */
@Serializable
data class RationRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val login: String,
)

/**
 * Представление ответа от WeightService на запрос пожеланий пользователя
 */
@Serializable
data class WishResponseDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val wish: WeightDesire
)

/**
 * Представление запроса на получение истории КБЖУ рационов
 */
@Serializable
data class HistoryRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val login: String,
    val days: Int = 30,
)

/**
 * Ответ от сервиса питания.
 * @property id Идентификатор пользователя.
 * @property rations Данные о питании.
 */
@Serializable
data class HistoryResponseDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val rations: Map<String, HistoryRow>
)

/**
 * Представление строки таблицы истории в запросе на получение истории КБЖУ рационов
 */
@Serializable
data class HistoryRow(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)

/**
 * Запрос от API Gateway
 * @property username Идентификатор пользователя.
 * @property weightControlWish Пожелание по контролю веса
 */
@Serializable
data class APIGatewayToWeightHistoryRequest(
    val username: String,
    val weightControlWish: WeightDesire = WeightDesire.REMAIN,
)

/**
 * Ответ от WeightHistoryService для API Gateway
 * @property username Идентификатор пользователя.
 * @property weightHistory Список записей с историей веса (ключ - дата/время, значение - вес)
 */
@Serializable
data class WeightHistoryResponse(
    val username: String,
    val weightHistory: Map<String, Float>,
)

/**
 * Сервис для работы с историей веса пользователя.
 * @property url URL базы данных.
 * @property user Имя пользователя базы данных.
 * @property password Пароль базы данных.
 */
class WeightHistoryService(
    internal var url: String = "jdbc:postgresql://localhost:5432/weightdb",
    internal var user: String = "postgres",
    internal var password: String = "password",
) {
    internal var username: String = ""
    internal var weightControlWish: WeightDesire = WeightDesire.REMAIN
    internal var weightHistory: List<WeightHistoryEntry> = emptyList()
    internal var activityData: Map<String, Double> = emptyMap()
    internal var nutritionData: Map<String, HistoryRow> = emptyMap()
    internal var age: Int = 30
    internal var gender: Gender = Gender.MALE
    internal var height: Int = 176
    internal var userDataReceived = CompletableDeferred<Unit>()
    internal var activityDataReceived = CompletableDeferred<Unit>()
    internal var nutritionDataReceived = CompletableDeferred<Unit>()
    internal var activityUUID:UUID = UUID.randomUUID()
    internal var userDataUUID:UUID = UUID.randomUUID()
    internal var nutritionUUID:UUID = UUID.randomUUID()

    /**
     * Обработчик запроса от NutritionService. Затем отправляет ответ.
     * Слушает по каналу "weight_history:request:WeightControlWish"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - username:String (id пользователя)
     * Отправляет по каналу "weight_history:response:WeightControlWish"
     * Отправляемые данные: закодированное Json.encodeToString - weightControlWish:String (текущее пожелание пользователя по контролю веса)
     */
    internal fun handleNutritionWishRequest(message: String) {
        try {
            val request = Json.decodeFromString<RationRequestDTO>(message)
            val wish = fetchWeightControlWishFromDB(request.login)
            sendEvent("weight_history:response:WeightControlWish", Json.encodeToString(WishResponseDTO(UUID.randomUUID(),wish)))
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle nutrition wish request", e)
        }
    }

    /**
     * Обработчик запроса от UserDataService.
     * Слушает по каналу "weight_history:request:NewWeight"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида NewWeightResponse(username: String, weightKg: Float) (id и новый вес пользователя)
     */
    internal fun handleNewWeightRequest(message: String) {
        try {
            val responseWrapper = Json.decodeFromString<ResponseWrapper<NewWeightResponse>>(message)
            val response = responseWrapper.dto
            validateWeight(response.weightKg)
            saveWeightToDB(username, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), response.weightKg)
            println("New weight saved for user: $username, weight: ${response.weightKg}, UUID: ${responseWrapper.id}")
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle new weight request", e)
        }
    }

    /**
     * Обработчик ответа от ActivityService.
     * Слушает по каналу "activity:response:CaloriesBurned"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида ActivityResponse(username:String, activities:List<ActivityRecord>),
     * где: ActivityRecord - это DTO вида ActivityRecord(date:String, calories:Double) (т.е. в сумме это id и список пар дата-сожжённые калории)
     */
    internal fun handleActivityResponse(message: String) {
        try {
            val responseWrapper = Json.decodeFromString<ResponseWrapper<ActivityResponse>>(message)
            if (responseWrapper.id != activityUUID) return
            val response = responseWrapper.dto
            activityData = response.activities.associate { it.date to it.calories }
            println("Activity data received for user: ${response.username}")
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle activity response", e)
        } finally {
            activityDataReceived.complete(Unit)
        }
    }

    /**
     * Обработчик ответа от UserDataService.
     * Слушает по каналу "user_data:response:UserData"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида UserDataResponse(username:String, weight:Float, age:Int, gender:String),
     * где: gender = {"male","female"} (т.е. в сумме - id, вес, возраст и пол пользователя)
     */
    internal fun handleUserDataResponse(message: String) {
        try {
            val responseWrapper = Json.decodeFromString<ResponseWrapper<UserDataResponse>>(message)
            if (responseWrapper.id != userDataUUID) return
            val response = responseWrapper.dto
            this.age = response.age
            validateWeight(response.weight)
            saveWeightToDB(username, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), response.weight)
            this.gender = response.gender
            this.height = response.height
            println("User data received: $response")
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle user data response", e)
        } finally {
            userDataReceived.complete(Unit)
        }
    }

    /**
     * Обработчик ответа от NutritionService.
     * Слушает по каналу "nutrition:response:CPFC" (CPFC - это КБЖУ)
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида NutritionResponse(username:String, nutritionData:Map<String, Map<String, Double>>),
     * где: nutritionData = словарь(Map) с ключом (String - датой формата yyyy-mm-dd) и значением - словарь(Map) c ключом(String) и значением(Double). Вот пример такого списка:
     * mapOf("2024-03-01" to mapOf("calories" to 2500.0, "protein" to 120.0, "fat" to 80.0, "carbs" to 300.0),
     *     "2024-03-02" to mapOf("calories" to 2300.0, "protein" to 110.0, "fat" to 70.0, "carbs" to 280.0))
     * Это то как я себе представил КБЖУ из NutritionService, можно и по другому реализовать, готов к предложениям
     */
    internal fun handleNutritionResponse(message: String) {
        try {
            val response = Json.decodeFromString<HistoryResponseDTO>(message)
            if (response.id != nutritionUUID) return
            this.nutritionData = response.rations
            println("Nutrition data received")
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle nutrition response", e)
        } finally {
            nutritionDataReceived.complete(Unit)
        }
    }

    /**
     * Обработчик запроса от API Gateway. Затем отправляет ответ.
     * Слушает по каналу "weight_history:request:WeightHistoryAndPrediction"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида APIGatewayToWeightHistoryRequest(username:String,weightControlWish: String = "keep")
     * Отправляет по каналу "weight_history:response:WeightHistoryAndPrediction"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида WeightHistoryResponse(username:String, weightHistory: Map<String, Float>),
     * где в Map<String, Double> String - дата/время, а Weight - это вес пользователя
     */
    internal fun handleAPIGatewayRequest(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val requestWrapper = Json.decodeFromString<UUIDWrapper<APIGatewayToWeightHistoryRequest>>(message)
            val request = requestWrapper.dto
            val result = processRequest(request.username, request.weightControlWish)
            println("Result of request from API Gateway: $result")
            sendEvent("weight_history:response:WeightHistoryAndPrediction", Json.encodeToString(UUIDWrapper(UUID.randomUUID(),result)))
        }
    }

    /**
     * Основная функция для запуска сервиса.
     */
    fun main(): Unit =
        runServiceListener(
            mapOf(
                "activity:response:CaloriesBurned" to ::handleActivityResponse,
                "user_data:response:UserData" to ::handleUserDataResponse,
                "nutrition:response:CPFC" to ::handleNutritionResponse,
                "weight_history:request:WeightControlWish" to ::handleNutritionWishRequest,
                "weight_history:request:NewWeight" to ::handleNewWeightRequest,
                "weight_history:request:WeightHistoryAndPrediction" to ::handleAPIGatewayRequest,
            ),
        )

    /**
     * Обрабатывает запрос на получение истории веса и прогноза.
     * @param username Идентификатор пользователя.
     * @param weightControlWish Пожелание по контролю веса (по умолчанию "keep").
     * @return Результат обработки запроса.
     */
    internal suspend fun processRequest(
        username: String,
        weightControlWish: WeightDesire = WeightDesire.REMAIN,
    ): WeightHistoryResponse {
        try {
            this.username = username
            this.weightControlWish = weightControlWish

            saveWeightControlWishToDB(username, weightControlWish)
            this.weightHistory = fetchWeightHistoryFromDB(username)

            fetchActivityData(username)
            fetchUserData(username)
            fetchNutritionData(username)

            val predictedWeight = calculatePredictedWeight()
            addPredictedWeight(predictedWeight)

            return WeightHistoryResponse(
                username = username,
                weightHistory = weightHistory.associate { it.dateTime to it.weight },
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to process request", e)
        }
    }

    /**
     * Сохраняет пожелание по контролю веса в базу данных.
     * @param username Идентификатор пользователя.
     * @param wish Пожелание по контролю веса.
     */
    internal fun saveWeightControlWishToDB(
        username: String,
        wish: WeightDesire,
    ) {
        try {
            DriverManager.getConnection(url, user, password).use { conn ->
                conn.prepareStatement(
                    """
                    CREATE TABLE IF NOT EXISTS user_settings (
                        user_id TEXT PRIMARY KEY,
                        weight_control_wish TEXT,
                        weight_history JSONB
                    )
                """,
                ).execute()

                conn.prepareStatement(
                    """
                    INSERT INTO user_settings (user_id, weight_control_wish)
                    VALUES (?, ?)
                    ON CONFLICT (user_id) DO UPDATE SET
                        weight_control_wish = EXCLUDED.weight_control_wish
                """,
                ).apply {
                    setString(1, username)
                    setString(2, wish.toString())
                    executeUpdate()
                }
            }
            println("Weight control wish saved for user: $username")
        } catch (e: SQLException) {
            throw RuntimeException("Failed to save weight control wish", e)
        }
    }

    /**
     * Получает пожелание по контролю веса из базы данных.
     * @param username Идентификатор пользователя.
     * @return Пожелание по контролю веса.
     */
    internal fun fetchWeightControlWishFromDB(username: String): WeightDesire {
        return try {
            DriverManager.getConnection(url, user, password).use { conn ->
                conn.prepareStatement(
                    """
                SELECT weight_control_wish FROM user_settings WHERE user_id = ?
            """,
                ).apply {
                    setString(1, username)
                }.executeQuery().let { rs ->
                    if (rs.next()) {
                        when (rs.getString("weight_control_wish")) {
                            "LOSS" -> WeightDesire.LOSS
                            "GAIN" -> WeightDesire.GAIN
                            "REMAIN" -> WeightDesire.REMAIN
                            else -> WeightDesire.REMAIN // default value
                        }
                    } else {
                        WeightDesire.REMAIN // default value
                    }
                }
            }
        } catch (e: SQLException) {
            throw RuntimeException("Failed to fetch weight control wish", e)
        }
    }

    /**
     * Сохраняет вес пользователя в базу данных.
     * @param username Идентификатор пользователя.
     * @param dateTime Дата и время записи.
     * @param weight Вес пользователя.
     */
    internal fun saveWeightToDB(
        username: String,
        dateTime: String,
        weight: Float,
    ) {
        try {
            validateWeight(weight)
            validateDateTime(dateTime)

            DriverManager.getConnection(url, user, password).use { conn ->
                val currentHistory = fetchWeightHistoryFromDB(username)
                val newHistory = currentHistory + WeightHistoryEntry(dateTime, weight)
                val jsonHistory = Json.encodeToString(newHistory)

                conn.prepareStatement(
                    """
                    INSERT INTO user_settings (user_id, weight_history)
                    VALUES (?, ?::JSONB)
                    ON CONFLICT (user_id) DO UPDATE SET
                        weight_history = EXCLUDED.weight_history
                """,
                ).apply {
                    setString(1, username)
                    setString(2, jsonHistory)
                    executeUpdate()
                }
            }
            println("Weight saved for user: $username")
        } catch (e: SQLException) {
            throw RuntimeException("Failed to save weight to database", e)
        }
    }

    /**
     * Получает историю веса пользователя из базы данных.
     * @param username Идентификатор пользователя.
     * @return Список записей истории веса.
     */
    internal fun fetchWeightHistoryFromDB(username: String): List<WeightHistoryEntry> {
        return try {
            DriverManager.getConnection(url, user, password).use { conn ->
                conn.prepareStatement(
                    """
                    SELECT weight_history FROM user_settings WHERE user_id = ?
                """,
                ).apply {
                    setString(1, username)
                }.executeQuery().let { rs ->
                    if (rs.next()) {
                        Json.decodeFromString(rs.getString("weight_history"))
                    } else {
                        emptyList()
                    }
                }
            }
        } catch (e: SQLException) {
            throw RuntimeException("Failed to fetch weight history", e)
        }
    }

    /**
     * Запрашивает данные о активности пользователя.
     * Отправляет запрос сервису ActivityService
     * Отправляет по каналу "activity:request:caloriesBurned"
     * @param username Идентификатор пользователя.
     * Отправляемые данные: закодированное Json.encodeToString - username:String (id пользователя)
     */
    internal suspend fun fetchActivityData(username: String) {
        try {
            activityDataReceived = CompletableDeferred()
            activityUUID = UUID.randomUUID()
            sendEvent("activity:request:caloriesBurned", Json.encodeToString(RequestWrapper(activityUUID,username)))
            println("Activity data requested for user: $username")
            activityDataReceived.await()
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch activity data", e)
        }
    }

    /**
     * Запрашивает данные о пользователе.
     * Отправляет запрос сервису UserDataService
     * Отправляет по каналу "user_data:request:UserData"
     * @param username Идентификатор пользователя.
     * Отправляемые данные: закодированное Json.encodeToString - username:String (id пользователя)
     */
    internal suspend fun fetchUserData(username: String) {
        try {
            userDataReceived = CompletableDeferred()
            userDataUUID = UUID.randomUUID()
            sendEvent("user_data:request:UserData", Json.encodeToString(GetterDto(userDataUUID, username)))
            println("User data requested for user: $username")
            userDataReceived.await()
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch user data", e)
        }
    }

    /**
     * Запрашивает данные о питании пользователя.
     * Отправляет запрос сервису NutritionService
     * Отправляет по каналу "nutrition:request:CPFC"
     * @param username Идентификатор пользователя.
     * Отправляемые данные: закодированное Json.encodeToString - username:String (id пользователя)
     */
    internal suspend fun fetchNutritionData(username: String) {
        try {
            nutritionDataReceived = CompletableDeferred()
            nutritionUUID = UUID.randomUUID()
            sendEvent("nutrition:request:CPFC", Json.encodeToString(HistoryRequestDTO(nutritionUUID,username)))
            println("Nutrition data requested for user: $username")
            nutritionDataReceived.await()
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch nutrition data", e)
        }
    }

    /**
     * Рассчитывает прогнозируемый вес пользователя.
     * @return Прогнозируемый вес.
     */
    internal fun calculatePredictedWeight(): Float {
        if (weightHistory.isEmpty()) return 0.0F

        val predictor =
            WeightPredictor(
                gender = gender,
                age = age,
                height = height,
                goal = weightControlWish,
            )

        weightHistory.forEach { entry ->
            val caloriesIntake = nutritionData[entry.dateTime.split(" ")[0]]?.calories ?: 2000.0

            val caloriesBurned = activityData[entry.dateTime.split(" ")[0]] ?: 0.0

            predictor.addRecord(entry.weight.toDouble(), caloriesIntake, caloriesBurned)
        }

        return predictor.predictNextWeight().toFloat()
    }

    /**
     * Добавляет прогнозируемый вес в историю.
     * @param weight Прогнозируемый вес.
     */
    internal fun addPredictedWeight(weight: Float) {
        val nextDate =
            LocalDateTime.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        weightHistory += WeightHistoryEntry(nextDate, weight)
    }

    /**
     * Проверяет корректность веса.
     * @param weight Вес пользователя.
     * @throws IllegalArgumentException Если вес некорректен.
     */
    internal fun validateWeight(weight: Float) {
        if (weight <= 0) {
            throw IllegalArgumentException("Weight must be positive")
        }
    }

    /**
     * Проверяет корректность даты и времени.
     * @param dateTime Дата и время.
     * @throws IllegalArgumentException Если формат даты некорректен.
     */
    internal fun validateDateTime(dateTime: String) {
        try {
            LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid date format", e)
        }
    }
}

fun main() {
    WeightHistoryService().main()
}
