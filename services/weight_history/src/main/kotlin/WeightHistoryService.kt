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
import java.time.format.DateTimeFormatter

/**
 * Ответ от сервиса активности.
 * @property userId Идентификатор пользователя.
 * @property activities Список записей активности.
 */
@Serializable
data class ActivityResponse(
    val userId: String,
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
data class WeightHistoryEntry(val dateTime: String, val weight: Double)

/**
 * Ответ от сервиса пользовательских данных.
 * @property userId Идентификатор пользователя.
 * @property weight Вес пользователя.
 * @property age Возраст пользователя.
 * @property gender Пол пользователя.
 */
@Serializable
data class UserDataResponse(
    val userId: String,
    val weight: Double,
    val age: Int,
    val gender: String,
)

/**
 * Ответ от сервиса питания.
 * @property userId Идентификатор пользователя.
 * @property nutritionData Данные о питании.
 */
@Serializable
data class NutritionResponse(
    val userId: String,
    val nutritionData: List<Pair<String, Map<String, Double>>>,
)

/**
 * Запрос от API Gateway
 * @property userId Идентификатор пользователя.
 * @property weightControlWish Пожелание по контролю веса
 */
@Serializable
data class APIGatewayToWeightHistoryRequest(
    val userId: String,
    val weightControlWish: String = "keep",
)

/**
 * Ответ от WeightHistoryService для API Gateway
 * @property userId Идентификатор пользователя.
 * @property weightHistory Список записей с историей веса (ключ - дата/время, значение - вес)
 */
@Serializable
data class WeightHistoryResponse(
    val userId: String,
    val weightHistory: Map<String, Double>
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
    internal var userId: String = ""
    internal var weightControlWish: String = "keep"
    internal var weightHistory: List<WeightHistoryEntry> = emptyList()
    internal var activityData: Map<String, Double> = emptyMap()
    internal var nutritionData: List<Pair<String, Map<String, Double>>> = emptyList()
    internal var age: Int = 30
    internal var gender: String = "male"
    internal var height: Int = 176
    internal var userDataReceived = CompletableDeferred<Unit>()
    internal var activityDataReceived = CompletableDeferred<Unit>()
    internal var nutritionDataReceived = CompletableDeferred<Unit>()

    /**
     * Обработчик запроса от NutritionService. Затем отправляет ответ.
     * Слушает по каналу "weight_history:request:WeightControlWish"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - userId:String (id пользователя)
     * Отправляет по каналу "weight_history:response:WeightControlWish"
     * Отправляемые данные: закодированное Json.encodeToString - weightControlWish:String (текущее пожелание пользователя по контролю веса)
     */
    internal fun handleNutritionWishRequest(message: String) {
        try {
            val userId = Json.decodeFromString<String>(message)
            val wish = fetchWeightControlWishFromDB(userId)
            sendEvent("weight_history:response:WeightControlWish", Json.encodeToString(wish))
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle nutrition wish request", e)
        }
    }

    /**
     * Обработчик запроса от UserDataService.
     * Слушает по каналу "weight_history:request:NewWeight"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - Pair(userId:String, weight:Double) (пара id и нового веса пользователя)
     */
    internal fun handleNewWeightRequest(message: String) {
        try {
            val (userId, weight) = Json.decodeFromString<Pair<String, Double>>(message)
            validateWeight(weight)
            saveWeightToDB(userId, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), weight)
            println("New weight saved for user: $userId, weight: $weight")
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle new weight request", e)
        }
    }

    /**
     * Обработчик ответа от ActivityService.
     * Слушает по каналу "activity:response:CaloriesBurned"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида ActivityResponse(userId:String, activities:List<ActivityRecord>),
     * где: ActivityRecord - это DTO вида ActivityRecord(date:String, calories:Double) (т.е. в сумме это id и список пар дата-сожжённые калории)
     */
    internal fun handleActivityResponse(message: String) {
        try {
            val response = Json.decodeFromString<ActivityResponse>(message)
            activityData = response.activities.associate { it.date to it.calories }
            println("Activity data received for user: ${response.userId}")
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle activity response", e)
        } finally {
            activityDataReceived.complete(Unit)
        }
    }

    /**
     * Обработчик ответа от UserDataService.
     * Слушает по каналу "user_data:response:UserData"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида UserDataResponse(userId:String, weight:Double, age:Int, gender:String),
     * где: gender = {"male","female"} (т.е. в сумме - id, вес, возраст и пол пользователя)
     */
    internal fun handleUserDataResponse(message: String) {
        try {
            val response = Json.decodeFromString<UserDataResponse>(message)
            this.age = response.age
            validateWeight(response.weight)
//            saveWeightToDB(userId, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), response.weight)
//            закомментировал, так как вызывает проблемы при одинаковых каналах взаимодействия с UserData
            this.gender = response.gender
            this.height = if (response.gender == "male") 176 else 164
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
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида NutritionResponse(userId:String, nutritionData:List<Pair<String, Map<String, Double>>>),
     * где: nutritionData = список(List) из пар(Pair): дата формата yyyy-mm-dd (String) и словаря(Map) c ключом(String) и значением(Double). Вот пример такого списка:
     * listOf("2024-03-01" to mapOf("calories" to 2500.0, "protein" to 120.0, "fat" to 80.0, "carbs" to 300.0),
     *        "2024-03-02" to mapOf("calories" to 2300.0, "protein" to 110.0, "fat" to 70.0, "carbs" to 280.0))
     * Это то как я себе представил КБЖУ из NutritionService, можно и по другому реализовать, готов к предложениям
     */
    internal fun handleNutritionResponse(message: String) {
        try {
            val response = Json.decodeFromString<NutritionResponse>(message)
            this.nutritionData = response.nutritionData
            println("Nutrition data received for user: ${response.userId}")
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle nutrition response", e)
        } finally {
            nutritionDataReceived.complete(Unit)
        }
    }

    /**
     * Обработчик запроса от API Gateway
     * Слушает по каналу "weight_history:request:WeightHistoryAndPrediction"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида APIGatewayToWeightHistoryRequest(userId:String,weightControlWish: String = "keep")
     * Отправляет по каналу "weight_history:response:WeightHistoryAndPrediction"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида WeightHistoryResponse(userId:String, weightHistory: Map<String, Double>),
     * где в Map<String, Double> String - дата/время, а Double - это вес пользователя
     */
    internal fun handleAPIGatewayRequest(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val request = Json.decodeFromString<APIGatewayToWeightHistoryRequest>(message)
            val result = processRequest(request.userId, request.weightControlWish)
            println("Result of request from API Gateway: $result")
            sendEvent("weight_history:response:WeightHistoryAndPrediction", Json.encodeToString(result))
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
     * @param userId Идентификатор пользователя.
     * @param weightControlWish Пожелание по контролю веса (по умолчанию "keep").
     * @return Результат обработки запроса.
     */
    suspend fun processRequest(
        userId: String,
        weightControlWish: String = "keep",
    ): WeightHistoryResponse {
        try {
            this.userId = userId
            this.weightControlWish = weightControlWish

            saveWeightControlWishToDB(userId, weightControlWish)
            this.weightHistory = fetchWeightHistoryFromDB(userId)

            fetchActivityData(userId)
            fetchUserData(userId)
            fetchNutritionData(userId)

            val predictedWeight = calculatePredictedWeight()
            addPredictedWeight(predictedWeight)

            return WeightHistoryResponse(
                userId = userId,
                weightHistory = weightHistory.associate { it.dateTime to it.weight }
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to process request", e)
        }
    }

    /**
     * Сохраняет пожелание по контролю веса в базу данных.
     * @param userId Идентификатор пользователя.
     * @param wish Пожелание по контролю веса.
     */
    internal fun saveWeightControlWishToDB(
        userId: String,
        wish: String,
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
                    setString(1, userId)
                    setString(2, wish)
                    executeUpdate()
                }
            }
            println("Weight control wish saved for user: $userId")
        } catch (e: SQLException) {
            throw RuntimeException("Failed to save weight control wish", e)
        }
    }

    /**
     * Получает пожелание по контролю веса из базы данных.
     * @param userId Идентификатор пользователя.
     * @return Пожелание по контролю веса.
     */
    internal fun fetchWeightControlWishFromDB(userId: String): String {
        return try {
            DriverManager.getConnection(url, user, password).use { conn ->
                conn.prepareStatement(
                    """
                    SELECT weight_control_wish FROM user_settings WHERE user_id = ?
                """,
                ).apply {
                    setString(1, userId)
                }.executeQuery().let { rs ->
                    if (rs.next()) rs.getString("weight_control_wish") ?: "keep" else "keep"
                }
            }
        } catch (e: SQLException) {
            throw RuntimeException("Failed to fetch weight control wish", e)
        }
    }

    /**
     * Сохраняет вес пользователя в базу данных.
     * @param userId Идентификатор пользователя.
     * @param dateTime Дата и время записи.
     * @param weight Вес пользователя.
     */
    internal fun saveWeightToDB(
        userId: String,
        dateTime: String,
        weight: Double,
    ) {
        try {
            validateWeight(weight)
            validateDateTime(dateTime)

            DriverManager.getConnection(url, user, password).use { conn ->
                val currentHistory = fetchWeightHistoryFromDB(userId)
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
                    setString(1, userId)
                    setString(2, jsonHistory)
                    executeUpdate()
                }
            }
            println("Weight saved for user: $userId")
        } catch (e: SQLException) {
            throw RuntimeException("Failed to save weight to database", e)
        }
    }

    /**
     * Получает историю веса пользователя из базы данных.
     * @param userId Идентификатор пользователя.
     * @return Список записей истории веса.
     */
    internal fun fetchWeightHistoryFromDB(userId: String): List<WeightHistoryEntry> {
        return try {
            DriverManager.getConnection(url, user, password).use { conn ->
                conn.prepareStatement(
                    """
                    SELECT weight_history FROM user_settings WHERE user_id = ?
                """,
                ).apply {
                    setString(1, userId)
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
     * Отправляет по каналу "request_activity_data"
     * @param userId Идентификатор пользователя.
     * Отправляемые данные: закодированное Json.encodeToString - userId:String (id пользователя)
     */
    internal suspend fun fetchActivityData(userId: String) {
        try {
            activityDataReceived = CompletableDeferred()
            sendEvent("request_activity_data", Json.encodeToString(userId))
            println("Activity data requested for user: $userId")
            activityDataReceived.await()
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch activity data", e)
        }
    }

    /**
     * Запрашивает данные о пользователе.
     * Отправляет запрос сервису UserDataService
     * Отправляет по каналу "request_user_data"
     * @param userId Идентификатор пользователя.
     * Отправляемые данные: закодированное Json.encodeToString - userId:String (id пользователя)
     */
    internal suspend fun fetchUserData(userId: String) {
        try {
            userDataReceived = CompletableDeferred()
            sendEvent("request_user_data", Json.encodeToString(userId))
            println("User data requested for user: $userId")
            userDataReceived.await()
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch user data", e)
        }
    }

    /**
     * Запрашивает данные о питании пользователя.
     * Отправляет запрос сервису NutritionService
     * Отправляет по каналу "request_nutrition_data"
     * @param userId Идентификатор пользователя.
     * Отправляемые данные: закодированное Json.encodeToString - userId:String (id пользователя)
     */
    internal suspend fun fetchNutritionData(userId: String) {
        try {
            nutritionDataReceived = CompletableDeferred()
            sendEvent("request_nutrition_data", Json.encodeToString(userId))
            println("Nutrition data requested for user: $userId")
            nutritionDataReceived.await()
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch nutrition data", e)
        }
    }

    /**
     * Рассчитывает прогнозируемый вес пользователя.
     * @return Прогнозируемый вес.
     */
    internal fun calculatePredictedWeight(): Double {
        if (weightHistory.isEmpty()) return 0.0

        val predictor =
            WeightPredictor(
                gender = gender,
                age = age,
                height = height,
                goal = weightControlWish,
            )

        weightHistory.forEach { entry ->
            val caloriesIntake =
                nutritionData
                    .firstOrNull { it.first == entry.dateTime.split(" ")[0] }
                    ?.second?.get("calories") ?: 2000.0

            val caloriesBurned = activityData[entry.dateTime.split(" ")[0]] ?: 0.0

            predictor.addRecord(entry.weight, caloriesIntake, caloriesBurned)
        }

        return predictor.predictNextWeight()
    }

    /**
     * Добавляет прогнозируемый вес в историю.
     * @param weight Прогнозируемый вес.
     */
    internal fun addPredictedWeight(weight: Double) {
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
    internal fun validateWeight(weight: Double) {
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
