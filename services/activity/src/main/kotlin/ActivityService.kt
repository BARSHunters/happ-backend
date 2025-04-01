import keydb.runServiceListener
import keydb.sendEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import utils.Gender
import utils.LocalDateSerializer
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import utils.UUIDSerializer
import utils.WeightDesire
import java.time.LocalDate

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

@Serializable
data class HeartRateEntry(val timestamp: Long, val heartRate: Int)

@Serializable
data class WorkoutData(val duration: String, val heartRates: List<HeartRateEntry>)

@Serializable
data class ActivityResponse(
    val userId: String,
    val activities: List<ActivityRecord>,
)

@Serializable
data class ActivityRecord(
    val date: String,
    val calories: Double,
)

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

@Serializable
data class TrainingData(
    val userId: String,
    val trainingDate: String,
    val trainingDuration: Int,
    val avgHeartRate: Double,
    val maxHeartRate: Int,
    val caloriesBurned: Double,
    val met: Double,
    val recoveryTime: Int,
)

/**
 * Представление запроса индекса активности пользователя от Nutrition
 */
@Serializable
data class RationRequestDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val login: String,
)

/**
 * Представление ответа от ActivityService на запрос индекса активности пользователя
 */
@Serializable
data class ActivityResponseDTO(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val activityIndex: Float
)

/**
 * Запрос от API Gateway
 * @param userId Идентификатор пользователя.
 * @param jsonWorkout JSON-строка с данными о тренировке (опционально).
 * @param trainingDate Дата тренировки (опционально).
 */
@Serializable
data class APIGatewayToActivityRequest(
    val userId: String,
    val jsonWorkout: String? = null,
    val trainingDate: String? = null,
)

class ActivityService(
    internal var url: String = "jdbc:postgresql://localhost:5432/trainingdb",
    internal var user: String = "postgres",
    internal var password: String = "password",
) {
    internal var userId: String = ""
    internal var trainingDate: String = ""
    internal var trainingDuration: Int = 0
    internal var heartRateList: List<Pair<Long, Int>> = emptyList()
    internal var avgHeartRate: Double = 0.0
    internal var maxHeartRate: Int = 0
    internal var weight: Float = 0.0F
    internal var age: Int = 0
    internal var gender: Gender = Gender.MALE
    internal var caloriesBurned: Double = 0.0
    internal var met: Double = 0.0
    internal var recoveryTime: Int = 0
    internal var userDataReceived = CompletableDeferred<Unit>()
    internal var userDataUUID = UUID.randomUUID()

    /**
     * Обработчик запроса от WeightHistoryService. Затем отправляет ответ
     * Слушает по каналу "activity:request:caloriesBurned"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - userId:String (id пользователя)
     * Отправляет по каналу "activity:response:CaloriesBurned"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида ActivityResponse(userId:String, activities:List<ActivityRecord>),
     * где: ActivityRecord - это DTO вида ActivityRecord(date:String, calories:Double) (т.е. в сумме это id и список пар дата-сожжённые калории)
     */
    internal fun handleActivityRequest(message: String) {
        val requestWrapper = Json.decodeFromString<RequestWrapper<String>>(message)
        val userId = requestWrapper.dto
        val records =
            fetchFromDatabase(userId).map {
                ActivityRecord(
                    date = it.trainingDate,
                    calories = it.caloriesBurned,
                )
            }
        sendEvent("activity:response:CaloriesBurned", Json.encodeToString(ResponseWrapper(requestWrapper.id,ActivityResponse(userId, records))))
    }

    /**
     * Обработчик ответа от UserDataService.
     * Слушает по каналу "user_data:response:UserData"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида UserDataResponse(userId:String, weight:Double, age:Int, gender:String),
     * где: gender = {"male","female"} (т.е. в сумме - id, вес, возраст и пол пользователя)
     */
    internal fun handleUserDataResponse(message: String) {
        val responseWrapper = Json.decodeFromString<ResponseWrapper<UserDataResponse>>(message)
        if (responseWrapper.id != userDataUUID) return
        val response = responseWrapper.dto
        this.weight = response.weight
        this.age = response.age
        this.gender = response.gender
        println("Received user data: $response")
        userDataReceived.complete(Unit)
    }

    /**
     * Обработчик запроса от API Gateway на добавление тренировки. Затем отправляет ответ.
     * Слушает по каналу "activity:request:AddTraining"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида APIGatewayToActivityRequest(userId:String, jsonWorkout:String? = null, trainingDate:String? = null)
     * Отправляет по каналу "activity:response:AddTraining"
     * Отправляемые данные: закодированное Json.encodeToString - String (сообщение о результате работы)
     */
    internal fun handleAddTrainingRequest(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val requestWrapper = Json.decodeFromString<UUIDWrapper<APIGatewayToActivityRequest>>(message)
            val request = requestWrapper.dto
            val result = processRequestAddTraining(request.userId, request.jsonWorkout, request.trainingDate)
            println("Result of request from API Gateway: $result")
            sendEvent("activity:response:AddTraining", Json.encodeToString(UUIDWrapper(UUID.randomUUID(),result)))
        }
    }

    /**
     * Обработчик запроса от API Gateway на получение некоторой тренировки пользователя по дате. Затем отправляет ответ.
     * Слушает по каналу "activity:request:GetSomeTraining"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида APIGatewayToActivityRequest(userId:String, jsonWorkout:String? = null, trainingDate:String? = null)
     * Отправляет по каналу "activity:response:GetSomeTraining"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида TrainingData(userId: String, trainingDate: String, trainingDuration: Int, avgHeartRate: Double,
     * maxHeartRate: Int, caloriesBurned: Double, met: Double, recoveryTime: Int) (т.е суммарно все сохраняемые в БД данные о тренировке)
     */
    internal fun handleGetSomeTrainingRequest(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val requestWrapper = Json.decodeFromString<UUIDWrapper<APIGatewayToActivityRequest>>(message)
            val request = requestWrapper.dto
            val result = processRequestGetSomeTraining(userId = request.userId, trainingDate = request.trainingDate)
            println("Result of request from API Gateway: $result")
            sendEvent("activity:response:GetSomeTraining", Json.encodeToString(UUIDWrapper(UUID.randomUUID(),result)))
        }
    }

    /**
     * Обработчик запроса от API Gateway на получение списка всех тренировок пользователя. Затем отправляет ответ.
     * Слушает по каналу "activity:request:GetAllTrainings"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида APIGatewayToActivityRequest(userId:String, jsonWorkout:String? = null, trainingDate:String? = null)
     * Отправляет по каналу "activity:response:GetAllTrainings"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида List<TrainingData> (т.е список всех данных о тренировках пользователя)
     */
    internal fun handleGetAllTrainingsRequest(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val requestWrapper = Json.decodeFromString<UUIDWrapper<APIGatewayToActivityRequest>>(message)
            val request = requestWrapper.dto
            val result = processRequestGetAllTraining(request.userId)
            println("Result of request from API Gateway: $result")
            sendEvent("activity:response:GetAllTrainings", Json.encodeToString(UUIDWrapper(UUID.randomUUID(),result)))
        }
    }

    /**
     * Обработчик запроса от NutritionService. Затем отправляет ответ.
     * Слушает по каналу "activity:request:ActivityIndex"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - login:String (id пользователя)
     * Отправляет по каналу "activity:response:ActivityIndex"
     * Отправляемые данные: закодированное Json.encodeToString - activityIndex:Float (текущее пожелание пользователя по контролю веса)
     */
    internal fun handleNutritionActivityIndexRequest(message: String) {
        try {
            val request = Json.decodeFromString<RationRequestDTO>(message)
            val met:Double = fetchFromDatabase(request.login).map { it.met }[0]
            val activityIndex = 1 + 0.05 * met
            sendEvent("activity:response:ActivityIndex", Json.encodeToString(ActivityResponseDTO(UUID.randomUUID(),activityIndex.toFloat())))
        } catch (e: Exception) {
            throw RuntimeException("Failed to handle nutrition wish request", e)
        }
    }

    fun main(): Unit =
        runServiceListener(
            mapOf(
                "activity:request:caloriesBurned" to ::handleActivityRequest,
                "user_data:response:UserData" to ::handleUserDataResponse,
                "activity:request:AddTraining" to ::handleAddTrainingRequest,
                "activity:request:GetSomeTraining" to::handleGetSomeTrainingRequest,
                "activity:request:GetAllTrainings" to::handleGetAllTrainingsRequest,
                "activity:request:ActivityIndex" to ::handleNutritionActivityIndexRequest,
            ),
        )

    /**
     * Обрабатывает запрос на добавление новой тренировки.
     * @param userId Идентификатор пользователя.
     * @param jsonWorkout JSON-строка с данными о тренировке (опционально).
     * @param trainingDate Дата тренировки (опционально).
     * @return Результат обработки запроса.
     */
    internal suspend fun processRequestAddTraining(
        userId: String,
        jsonWorkout: String? = null,
        trainingDate: String? = null,
    ): String {
        this.userId = userId
        if (jsonWorkout != null) {
            if (trainingDate != null) {
                this.trainingDate = trainingDate
            } else {
                this.trainingDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }
            parseWorkout(jsonWorkout)
            calculateHeartRateMetrics()
            fetchUserData()
            calculateCalories()
            calculateMET()
            calculateRecoveryTime()
            sendTrainingDataToAchievementAndNotifyService()
            saveToDatabase()
            return "Workout processed and saved."
        } else {
            return "You didn't give me workout data"
        }
    }

    /**
     * Обрабатывает запрос на получение некоторой тренировки пользователя по дате.
     * @param userId Идентификатор пользователя.
     * @param trainingDate Дата тренировки (опционально).
     * @return Результат обработки запроса.
     */
    internal fun processRequestGetSomeTraining(
        userId: String,
        trainingDate: String? = null,
    ): TrainingData {
        return fetchFromDatabase(userId, trainingDate)[0]
    }

    /**
     * Обрабатывает запрос на получение списка всех тренировок пользователя.
     * @param userId Идентификатор пользователя.
     * @return Результат обработки запроса.
     */
    internal fun processRequestGetAllTraining(userId: String): List<TrainingData> {
        return fetchFromDatabase(userId)
    }

    /**
     * Проверяет корректность данных о тренировке.
     * @param workout Данные о тренировке.
     * @throws IllegalArgumentException Если данные некорректны.
     */
    internal fun validateWorkoutData(workout: WorkoutData) {
        if (workout.duration.split(":").size != 3) {
            throw IllegalArgumentException("Invalid duration format, expected HH:MM:SS")
        }
        if (workout.heartRates.isEmpty()) {
            throw IllegalArgumentException("Heart rate data is empty")
        }
    }

    /**
     * Парсит JSON-строку с данными о тренировке.
     * @param jsonWorkout JSON-строка с данными о тренировке.
     */
    internal fun parseWorkout(jsonWorkout: String) {
        try {
            val workout = Json.decodeFromString<WorkoutData>(jsonWorkout)
            validateWorkoutData(workout)

            val parts = workout.duration.split(":").map { it.toInt() }

            this.trainingDuration = parts[0] * 3600 + parts[1] * 60 + parts[2] // Перевод в секунды
            this.heartRateList = workout.heartRates.map { it.timestamp to it.heartRate }
        } catch (e: SerializationException) {
            println("JSON decoding error: ${e.message}")
            throw RuntimeException("Failed to parse workout data", e)
        } catch (e: IllegalArgumentException) {
            println("Invalid duration format: ${e.message}")
            throw RuntimeException("Invalid workout data", e)
        }
    }

    /**
     * Рассчитывает средний и максимальный пульс на основе данных о тренировке.
     */
    internal fun calculateHeartRateMetrics() {
        maxHeartRate = heartRateList.maxOf { it.second }

        var weightedSum = 0.0
        var totalTime = 0L

        for (i in heartRateList.indices) {
            val currentTimestamp = heartRateList[i].first
            val currentHeartRate = heartRateList[i].second

            val dtBefore = if (i > 0) (currentTimestamp - heartRateList[i - 1].first) / 2 else 0
            val dtAfter = if (i < heartRateList.size - 1) (heartRateList[i + 1].first - currentTimestamp) / 2 else 0

            val dt = dtBefore + dtAfter

            weightedSum += currentHeartRate * dt
            totalTime += dt
        }

        avgHeartRate = if (totalTime > 0) weightedSum / totalTime else 0.0
    }

    /**
     * Запрашивает данные о пользователе.
     * Отправляет запрос сервису UserDataService
     * Отправляет по каналу "user_data:request:UserData"
     * Отправляемые данные: закодированное Json.encodeToString - userId:String (id пользователя)
     */
    internal suspend fun fetchUserData() {
        try {
            userDataReceived = CompletableDeferred()
            userDataUUID = UUID.randomUUID()
            sendEvent("user_data:request:UserData", Json.encodeToString(GetterDto(userDataUUID,userId)))
            userDataReceived.await()
        } catch (e: Exception) {
            println("Failed to send event: ${e.message}")
            throw RuntimeException("Failed to fetch user data", e)
        }
    }

    /**
     * Проверяет корректность данных пользователя.
     * @throws IllegalArgumentException Если данные некорректны.
     */
    internal fun validateUserData() {
        if (weight <= 0 || age <= 0) {
            throw IllegalArgumentException("Invalid user data: weight or age is not positive")
        }
        if (gender !in listOf(Gender.MALE, Gender.FEMALE)) {
            throw IllegalArgumentException("Invalid user data: gender must be 'male' or 'female'")
        }
    }

    /**
     * Рассчитывает количество сожжённых калорий на основе данных о тренировке и пользователе.
     * @throws RuntimeException Если данные от пользователя некорректны.
     */
    internal fun calculateCalories() {
        validateUserData()
        this.caloriesBurned =
            if (gender == Gender.MALE) {
                ((-55.0969 + (0.6309 * avgHeartRate) + (0.1988 * weight) + (0.2017 * age)) / 4.184) * trainingDuration
            } else {
                ((-20.4022 + (0.4472 * avgHeartRate) - (0.1263 * weight) + (0.074 * age)) / 4.184) * trainingDuration
            }
    }

    /**
     * Рассчитывает метаболический эквивалент тренировки (MET).
     */
    internal fun calculateMET() {
        this.met = ((avgHeartRate - 60) / (220 - age - 60)) * 10
    }

    /**
     * Рассчитывает время восстановления после тренировки.
     */
    internal fun calculateRecoveryTime() {
        val baseTime =
            when {
                met < 4 -> 12
                met in 4.0..7.0 -> 24
                else -> 36
            }

        val intensityFactor = 1 + ((avgHeartRate - 0.6 * maxHeartRate) / (0.4 * maxHeartRate))
        this.recoveryTime = (baseTime * intensityFactor * 3600).toInt()
    }

    /**
     * Отправляет данные о тренировке.
     * Отправляет запрос в сервисы AchievementService и NotifyService
     * Отправляет по каналам "request_training_data" и "notify:request:TrainingData"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида TrainingData(userId: String, trainingDate: String, trainingDuration: Int, avgHeartRate: Double,
     * maxHeartRate: Int, caloriesBurned: Double, met: Double, recoveryTime: Int) (т.е суммарно все сохраняемые в БД данные о тренировке)
     */
    internal fun sendTrainingDataToAchievementAndNotifyService() {
        try {
            val trainingData =
                TrainingData(
                    userId = userId,
                    trainingDate = trainingDate,
                    trainingDuration = trainingDuration,
                    avgHeartRate = avgHeartRate,
                    maxHeartRate = maxHeartRate,
                    caloriesBurned = caloriesBurned,
                    met = met,
                    recoveryTime = recoveryTime,
                )
            sendEvent("achievement:request:TrainingData", Json.encodeToString(UUIDWrapper(UUID.randomUUID(),trainingData)))
            sendEvent("notify:request:TrainingData", Json.encodeToString(UUIDWrapper(UUID.randomUUID(),trainingData)))
        } catch (e: Exception) {
            println("Failed to send training data: ${e.message}")
            throw RuntimeException("Failed to send training data to Achievement and Notify services", e)
        }
    }

    /**
     * Сохраняет данные о тренировке в базу данных.
     */
    internal fun saveToDatabase() {
        try {
            val connection = DriverManager.getConnection(url, user, password)
            val statement =
                connection.prepareStatement(
                    """
            CREATE TABLE IF NOT EXISTS activity (
                user_id TEXT,
                training_date TIMESTAMP,
                training_duration INT,
                avg_heart_rate DOUBLE PRECISION,
                max_heart_rate INT,
                calories_burned DOUBLE PRECISION,
                MET DOUBLE PRECISION,
                recovery_time INT
            )
        """,
                )
            statement.execute()
            statement.close()

            val insertStatement =
                connection.prepareStatement(
                    """
            INSERT INTO activity (user_id, training_date, training_duration, avg_heart_rate, max_heart_rate, calories_burned, MET, recovery_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
                )
            insertStatement.setString(1, userId)
            insertStatement.setTimestamp(2, Timestamp.valueOf(trainingDate))
            insertStatement.setInt(3, trainingDuration)
            insertStatement.setDouble(4, avgHeartRate)
            insertStatement.setInt(5, maxHeartRate)
            insertStatement.setDouble(6, caloriesBurned)
            insertStatement.setDouble(7, met)
            insertStatement.setInt(8, recoveryTime)
            insertStatement.executeUpdate()
            insertStatement.close()

            connection.close()
        } catch (e: SQLException) {
            println("Database error: ${e.message}")
            throw RuntimeException("Failed to save training data to database", e)
        }
    }

    /**
     * Получает данные о тренировках пользователя из базы данных.
     * @param userId Идентификатор пользователя.
     * @param trainingDate Дата тренировки (опционально).
     * @return Список данных о тренировках.
     */
    internal fun fetchFromDatabase(
        userId: String,
        trainingDate: String? = null,
    ): List<TrainingData> {
        return try {
            val connection = DriverManager.getConnection(url, user, password)

            val query =
                if (trainingDate != null) {
                    "SELECT * FROM activity WHERE user_id = ? AND training_date = ?"
                } else {
                    "SELECT * FROM activity WHERE user_id = ?"
                }

            val statement = connection.prepareStatement(query)
            statement.setString(1, userId)

            if (trainingDate != null) {
                statement.setTimestamp(2, Timestamp.valueOf(trainingDate))
            }

            val resultSet = statement.executeQuery()
            val result = mutableListOf<TrainingData>()

            while (resultSet.next()) {
                val record =
                    TrainingData(
                        userId = resultSet.getString("user_id"),
                        trainingDate = resultSet.getTimestamp("training_date").toString(),
                        trainingDuration = resultSet.getInt("training_duration"),
                        avgHeartRate = resultSet.getDouble("avg_heart_rate"),
                        maxHeartRate = resultSet.getInt("max_heart_rate"),
                        caloriesBurned = resultSet.getDouble("calories_burned"),
                        met = resultSet.getDouble("MET"),
                        recoveryTime = resultSet.getInt("recovery_time"),
                    )
                result.add(record)
            }

            statement.close()
            connection.close()

            result
        } catch (e: SQLException) {
            println("Database error: ${e.message}")
            throw RuntimeException("Failed to fetch training data from database", e)
        }
    }
}

fun main() {
    ActivityService().main()
}
