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
import utils.UUIDSerializer
import utils.WeightDesire
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Оболочка для поддержки запросов с UUID (Слава)
 */
@Serializable
data class RequestWrapper<T>(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val dto: T,
)

/**
 * Оболочка для поддержки ответов с UUID (Слава)
 */
@Serializable
data class ResponseWrapper<T>(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val dto: T,
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
data class WorkoutData(
    val duration: String,
    val datetime: String,
    val name: String,
    val intensityZones: List<Int>,
    val heartRates: List<HeartRateEntry>
)

@Serializable
data class ActivityResponse(
    val username: String,
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
    val weightDesire: WeightDesire,
)

@Serializable
data class TrainingData(
    val username: String,
    val trainingName: String,
    val trainingDate: String,
    val trainingDuration: Int,
    val avgHeartRate: Double,
    val maxHeartRate: Int,
    val caloriesBurned: Double,
    val met: Double,
    val intensityZones: List<Int>,
    val recoveryTime: Int,
)

// Специальный тип данных, объект которого создаётся при каждом новом запросе на добавление тренировки
data class ExtendedTrainingData(
    internal var username: String = "",
    var trainingDate: String = "",
    var trainingName: String = "",
    var trainingDuration: Int = 0,
    var heartRateList: List<Pair<Long, Int>> = emptyList(),
    var avgHeartRate: Double = 0.0,
    var maxHeartRate: Int = 0,
    var weight: Float = 0.0F,
    var age: Int = 0,
    var gender: Gender = Gender.MALE,
    var caloriesBurned: Double = 0.0,
    var met: Double = 0.0,
    var intensityZones: List<Int> = emptyList(),
    var recoveryTime: Int = 0,
    var userDataReceived: CompletableDeferred<Unit> = CompletableDeferred(),
    var userDataUUID: UUID = UUID.randomUUID(),
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
    val activityIndex: Float,
)

/**
 * Запрос от API Gateway
 * @param username Идентификатор пользователя.
 * @param jsonWorkout JSON-строка с данными о тренировке (опционально).
 * @param startTrainingDate Начальная дата периода тренировок (опционально).
 * @param endTrainingDate Конечная дата периода тренировок (опционально).
 */
@Serializable
data class APIGatewayToActivityRequest(
    val username: String,
    val jsonWorkout: String? = null,
    val startTrainingDate: String? = null,
    val endTrainingDate: String? = null,
)

class ActivityService(
    internal var url: String = "jdbc:postgresql://localhost:5432/trainingdb",
    internal var user: String = "postgres",
    internal var password: String = "password",
) {
    internal val requestTrainingDataList = ConcurrentLinkedQueue<ExtendedTrainingData>()

    /**
     * Обработчик запроса от WeightHistoryService. Затем отправляет ответ
     * Слушает по каналу "activity:request:caloriesBurned"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - username:String (id пользователя)
     * Отправляет по каналу "activity:response:CaloriesBurned"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида ActivityResponse(username:String, activities:List<ActivityRecord>),
     * где: ActivityRecord - это DTO вида ActivityRecord(date:String, calories:Double) (т.е. в сумме это id и список пар дата-сожжённые калории)
     */
    internal fun handleActivityRequest(message: String) {
        val requestWrapper = Json.decodeFromString<RequestWrapper<String>>(message)
        val username = requestWrapper.dto
        val records =
            fetchFromDatabase(username).map {
                ActivityRecord(
                    date = it.trainingDate,
                    calories = it.caloriesBurned,
                )
            }
        sendEvent(
            "activity:response:CaloriesBurned",
            Json.encodeToString(ResponseWrapper(requestWrapper.id, ActivityResponse(username, records))),
        )
    }

    /**
     * Обработчик ответа от UserDataService.
     * Слушает по каналу "user_data:response:UserData"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида UserDataResponse(username:String, weight:Double, age:Int, gender:String),
     * где: gender = {"male","female"} (т.е. в сумме - id, вес, возраст и пол пользователя)
     */
    internal fun handleUserDataResponse(message: String) {
        val responseWrapper = Json.decodeFromString<ResponseWrapper<UserDataResponse>>(message)
        val matchingRequestTrainingData = requestTrainingDataList.find { it.userDataUUID == responseWrapper.id }
        if (matchingRequestTrainingData == null) {
            println("No matching training request found for UUID: ${responseWrapper.id}")
            return
        }
        val response = responseWrapper.dto
        with(matchingRequestTrainingData) {
            weight = response.weight
            age = response.age
            gender = response.gender
        }
        println("Received user data for request ${responseWrapper.id}: $response")
        matchingRequestTrainingData.userDataReceived.complete(Unit)
    }

    /**
     * Обработчик запроса от API Gateway на добавление тренировки. Затем отправляет ответ.
     * Слушает по каналу "activity:request:AddTraining"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида APIGatewayToActivityRequest(username:String, jsonWorkout:String? = null, trainingDate:String? = null)
     * Отправляет по каналу "activity:response:AddTraining"
     * Отправляемые данные: закодированное Json.encodeToString - String (сообщение о результате работы)
     */
    internal fun handleAddTrainingRequest(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val requestWrapper = Json.decodeFromString<UUIDWrapper<APIGatewayToActivityRequest>>(message)
            val request = requestWrapper.dto
            val result = processRequestAddTraining(request.username, request.jsonWorkout)
            println("Result of request from API Gateway: $result")
            sendEvent("activity:response:AddTraining", Json.encodeToString(UUIDWrapper(UUID.randomUUID(), result)))
        }
    }

    /**
     * Обработчик запроса от API Gateway на получение некоторой тренировки пользователя по дате. Затем отправляет ответ.
     * Слушает по каналу "activity:request:GetSomeTraining"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида APIGatewayToActivityRequest(username:String, jsonWorkout:String? = null, trainingDate:String? = null)
     * Отправляет по каналу "activity:response:GetSomeTraining"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида TrainingData(username: String, trainingDate: String, trainingDuration: Int, avgHeartRate: Double,
     * maxHeartRate: Int, caloriesBurned: Double, met: Double, recoveryTime: Int) (т.е суммарно все сохраняемые в БД данные о тренировке)
     */
    internal fun handleGetSomeTrainingRequest(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val requestWrapper = Json.decodeFromString<UUIDWrapper<APIGatewayToActivityRequest>>(message)
            val request = requestWrapper.dto
            val result = processRequestGetSomeTraining(username = request.username, startTrainingDate = request.startTrainingDate, endTrainingDate = request.endTrainingDate)
            println("Result of request from API Gateway: $result")
            sendEvent("activity:response:GetSomeTraining", Json.encodeToString(UUIDWrapper(UUID.randomUUID(), result)))
        }
    }

    /**
     * Обработчик запроса от API Gateway на получение списка всех тренировок пользователя. Затем отправляет ответ.
     * Слушает по каналу "activity:request:GetAllTrainings"
     * @param message Ожидаемые данные: закодированное Json.encodeToString - DTO вида APIGatewayToActivityRequest(username:String, jsonWorkout:String? = null, trainingDate:String? = null)
     * Отправляет по каналу "activity:response:GetAllTrainings"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида List<TrainingData> (т.е список всех данных о тренировках пользователя)
     */
    internal fun handleGetAllTrainingsRequest(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val requestWrapper = Json.decodeFromString<UUIDWrapper<APIGatewayToActivityRequest>>(message)
            val request = requestWrapper.dto
            val result = processRequestGetAllTraining(request.username)
            println("Result of request from API Gateway: $result")
            sendEvent("activity:response:GetAllTrainings", Json.encodeToString(UUIDWrapper(UUID.randomUUID(), result)))
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
            val met: Double = fetchFromDatabase(request.login).map { it.met }[0]
            val activityIndex = 1 + 0.05 * met
            sendEvent(
                "activity:response:ActivityIndex",
                Json.encodeToString(ActivityResponseDTO(UUID.randomUUID(), activityIndex.toFloat())),
            )
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
     * @param username Идентификатор пользователя.
     * @param jsonWorkout JSON-строка с данными о тренировке (опционально).
     * @return Результат обработки запроса.
     */
    internal suspend fun processRequestAddTraining(
        username: String,
        jsonWorkout: String? = null,
    ): String {
        if (jsonWorkout != null) {
            val thisRequestTrainingData = ExtendedTrainingData()
            this.requestTrainingDataList.add(thisRequestTrainingData)
            thisRequestTrainingData.username = username
            parseWorkout(jsonWorkout, thisRequestTrainingData)
            if (thisRequestTrainingData.trainingDate == "") {
                thisRequestTrainingData.trainingDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }
            calculateHeartRateMetrics(thisRequestTrainingData)
            fetchUserData(thisRequestTrainingData)
            calculateCalories(thisRequestTrainingData)
            calculateMET(thisRequestTrainingData)
            calculateRecoveryTime(thisRequestTrainingData)
            sendTrainingDataToAchievementAndNotifyService(thisRequestTrainingData)
            saveToDatabase(thisRequestTrainingData)
            this.requestTrainingDataList.remove(thisRequestTrainingData)
            return "Workout processed and saved."
        } else {
            return "You didn't give me workout data"
        }
    }

    /**
     * Обрабатывает запрос на получение некоторой тренировки пользователя по дате.
     * @param username Идентификатор пользователя.
     * @param startTrainingDate Начальная дата периода тренировок (опционально).
     * @param endTrainingDate Конечная дата периода тренировок (опционально).
     * @return Результат обработки запроса.
     */
    internal fun processRequestGetSomeTraining(
        username: String,
        startTrainingDate: String? = null,
        endTrainingDate: String? = null,
    ): List<TrainingData> {
        return fetchFromDatabase(username, startTrainingDate, endTrainingDate)
    }

    /**
     * Обрабатывает запрос на получение списка всех тренировок пользователя.
     * @param username Идентификатор пользователя.
     * @return Результат обработки запроса.
     */
    internal fun processRequestGetAllTraining(username: String): List<TrainingData> {
        return fetchFromDatabase(username)
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
    internal fun parseWorkout(jsonWorkout: String, thisRequestTrainingData: ExtendedTrainingData) {
        try {
            val workout = Json.decodeFromString<WorkoutData>(jsonWorkout)
            validateWorkoutData(workout)

            val parts = workout.duration.split(":").map { it.toInt() }

            thisRequestTrainingData.apply {
                trainingDuration = parts[0] * 3600 + parts[1] * 60 + parts[2]
                trainingName = workout.name
                trainingDate = workout.datetime
                intensityZones = workout.intensityZones
                heartRateList = workout.heartRates.map { it.timestamp to it.heartRate }
            }
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
    internal fun calculateHeartRateMetrics(thisRequestTrainingData: ExtendedTrainingData) {
        thisRequestTrainingData.maxHeartRate = thisRequestTrainingData.heartRateList.maxOf { it.second }

        var weightedSum = 0.0
        var totalTime = 0L

        for (i in thisRequestTrainingData.heartRateList.indices) {
            val currentTimestamp = thisRequestTrainingData.heartRateList[i].first
            val currentHeartRate = thisRequestTrainingData.heartRateList[i].second

            val dtBefore = if (i > 0) (currentTimestamp - thisRequestTrainingData.heartRateList[i - 1].first) / 2 else 0
            val dtAfter = if (i < thisRequestTrainingData.heartRateList.size - 1) (thisRequestTrainingData.heartRateList[i + 1].first - currentTimestamp) / 2 else 0

            val dt = dtBefore + dtAfter

            weightedSum += currentHeartRate * dt
            totalTime += dt
        }

        thisRequestTrainingData.avgHeartRate = if (totalTime > 0) weightedSum / totalTime else 0.0
    }

    /**
     * Запрашивает данные о пользователе.
     * Отправляет запрос сервису UserDataService
     * Отправляет по каналу "user_data:request:UserData"
     * Отправляемые данные: закодированное Json.encodeToString - username:String (id пользователя)
     */
    internal suspend fun fetchUserData(thisRequestTrainingData: ExtendedTrainingData) {
        try {
            thisRequestTrainingData.userDataReceived = CompletableDeferred()
            thisRequestTrainingData.userDataUUID = UUID.randomUUID()
            sendEvent("user_data:request:UserData", Json.encodeToString(GetterDto(thisRequestTrainingData.userDataUUID, thisRequestTrainingData.username)))
            thisRequestTrainingData.userDataReceived.await()
        } catch (e: Exception) {
            println("Failed to send event: ${e.message}")
            throw RuntimeException("Failed to fetch user data", e)
        }
    }

    /**
     * Проверяет корректность данных пользователя.
     * @throws IllegalArgumentException Если данные некорректны.
     */
    internal fun validateUserData(thisRequestTrainingData: ExtendedTrainingData) {
        if (thisRequestTrainingData.weight <= 0 || thisRequestTrainingData.age <= 0) {
            throw IllegalArgumentException("Invalid user data: weight or age is not positive")
        }
        if (thisRequestTrainingData.gender !in listOf(Gender.MALE, Gender.FEMALE)) {
            throw IllegalArgumentException("Invalid user data: gender must be 'male' or 'female'")
        }
    }

    /**
     * Рассчитывает количество сожжённых калорий на основе данных о тренировке и пользователе.
     * @throws RuntimeException Если данные от пользователя некорректны.
     */
    internal fun calculateCalories(thisRequestTrainingData: ExtendedTrainingData) {
        validateUserData(thisRequestTrainingData)
        thisRequestTrainingData.caloriesBurned =
            if (thisRequestTrainingData.gender == Gender.MALE) {
                ((-55.0969 + (0.6309 * thisRequestTrainingData.avgHeartRate) + (0.1988 * thisRequestTrainingData.weight) + (0.2017 * thisRequestTrainingData.age)) / 4.184) * thisRequestTrainingData.trainingDuration
            } else {
                ((-20.4022 + (0.4472 * thisRequestTrainingData.avgHeartRate) - (0.1263 * thisRequestTrainingData.weight) + (0.074 * thisRequestTrainingData.age)) / 4.184) * thisRequestTrainingData.trainingDuration
            }
    }

    /**
     * Рассчитывает метаболический эквивалент тренировки (MET).
     */
    internal fun calculateMET(thisRequestTrainingData: ExtendedTrainingData) {
        thisRequestTrainingData.met = ((thisRequestTrainingData.avgHeartRate - 60) / (220 - thisRequestTrainingData.age - 60)) * 10
    }

    /**
     * Рассчитывает время восстановления после тренировки.
     */
    internal fun calculateRecoveryTime(thisRequestTrainingData: ExtendedTrainingData) {
        val baseTime =
            when {
                thisRequestTrainingData.met < 4 -> 12
                thisRequestTrainingData.met in 4.0..7.0 -> 24
                else -> 36
            }

        val intensityFactor = 1 + ((thisRequestTrainingData.avgHeartRate - 0.6 * thisRequestTrainingData.maxHeartRate) / (0.4 * thisRequestTrainingData.maxHeartRate))
        thisRequestTrainingData.recoveryTime = (baseTime * intensityFactor * 3600).toInt()
    }

    /**
     * Отправляет данные о тренировке.
     * Отправляет запрос в сервисы AchievementService и NotifyService
     * Отправляет по каналам "request_training_data" и "notify:request:TrainingData"
     * Отправляемые данные: закодированное Json.encodeToString - DTO вида TrainingData(username: String, trainingDate: String, trainingDuration: Int, avgHeartRate: Double,
     * maxHeartRate: Int, caloriesBurned: Double, met: Double, recoveryTime: Int) (т.е суммарно все сохраняемые в БД данные о тренировке)
     */
    internal fun sendTrainingDataToAchievementAndNotifyService(thisRequestTrainingData: ExtendedTrainingData) {
        try {
            val trainingData =
                TrainingData(
                    username = thisRequestTrainingData.username,
                    trainingName = thisRequestTrainingData.trainingName,
                    trainingDate = thisRequestTrainingData.trainingDate,
                    trainingDuration = thisRequestTrainingData.trainingDuration,
                    avgHeartRate = thisRequestTrainingData.avgHeartRate,
                    maxHeartRate = thisRequestTrainingData.maxHeartRate,
                    caloriesBurned = thisRequestTrainingData.caloriesBurned,
                    met = thisRequestTrainingData.met,
                    intensityZones = thisRequestTrainingData.intensityZones,
                    recoveryTime = thisRequestTrainingData.recoveryTime,
                )
            sendEvent("achievement:request:TrainingData", Json.encodeToString(UUIDWrapper(UUID.randomUUID(), trainingData)))
            sendEvent("notify:request:TrainingData", Json.encodeToString(UUIDWrapper(UUID.randomUUID(), trainingData)))
        } catch (e: Exception) {
            println("Failed to send training data: ${e.message}")
            throw RuntimeException("Failed to send training data to Achievement and Notify services", e)
        }
    }

    /**
     * Сохраняет данные о тренировке в базу данных.
     */
    internal fun saveToDatabase(thisRequestTrainingData: ExtendedTrainingData) {
        try {
            val connection = DriverManager.getConnection(url, user, password)

            // Обновляем запрос создания таблицы
            val createTableStatement = connection.prepareStatement(
                """
            CREATE TABLE IF NOT EXISTS activity (
                user_id TEXT,
                training_date TIMESTAMP,
                training_duration INT,
                training_name TEXT,                -- Новое поле
                intensity_zones INT[],             -- Новое поле (массив целых чисел)
                avg_heart_rate DOUBLE PRECISION,
                max_heart_rate INT,
                calories_burned DOUBLE PRECISION,
                MET DOUBLE PRECISION,
                recovery_time INT
            )
            """
            )
            createTableStatement.execute()
            createTableStatement.close()

            // Обновляем запрос вставки данных
            val insertStatement = connection.prepareStatement(
                """
            INSERT INTO activity (
                user_id, training_date, training_duration, training_name,
                intensity_zones, avg_heart_rate, max_heart_rate,
                calories_burned, MET, recovery_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
            ).apply {
                setString(1, thisRequestTrainingData.username)
                setTimestamp(2, Timestamp.valueOf(thisRequestTrainingData.trainingDate))
                setInt(3, thisRequestTrainingData.trainingDuration)
                setString(4, thisRequestTrainingData.trainingName)  // Новое поле

                // Преобразуем List<Int> в SQL массив
                val array = connection.createArrayOf("INTEGER",
                    thisRequestTrainingData.intensityZones.toTypedArray())
                setArray(5, array)

                setDouble(6, thisRequestTrainingData.avgHeartRate)
                setInt(7, thisRequestTrainingData.maxHeartRate)
                setDouble(8, thisRequestTrainingData.caloriesBurned)
                setDouble(9, thisRequestTrainingData.met)
                setInt(10, thisRequestTrainingData.recoveryTime)
            }

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
     * @param username Идентификатор пользователя.
     * @param startTrainingDate Начальная дата периода тренировок (опционально).
     * @param endTrainingDate Конечная дата периода тренировок (опционально).
     * @return Список данных о тренировках.
     */
    internal fun fetchFromDatabase(
        username: String,
        startTrainingDate: String? = null,
        endTrainingDate: String? = null,
    ): List<TrainingData> {
        return try {
            val connection = DriverManager.getConnection(url, user, password)

            val query =
                if (startTrainingDate != null && endTrainingDate != null) {
                    "SELECT * FROM activity WHERE user_id = ? AND training_date BETWEEN ? AND ?"
                } else {
                    "SELECT * FROM activity WHERE user_id = ?"
                }

            val statement = connection.prepareStatement(query)
            statement.setString(1, username)

            if (startTrainingDate != null) {
                statement.setTimestamp(2, Timestamp.valueOf(startTrainingDate))
            }
            if (endTrainingDate != null) {
                statement.setTimestamp(3, Timestamp.valueOf(endTrainingDate))
            }

            val resultSet = statement.executeQuery()
            val result = mutableListOf<TrainingData>()

            while (resultSet.next()) {
                val intensityZonesArray = resultSet.getArray("intensity_zones")
                val intensityZones = if (intensityZonesArray != null) {
                    (intensityZonesArray.array as? Array<*>)?.filterIsInstance<Int>() ?: emptyList()
                } else {
                    emptyList()
                }

                val record =
                    TrainingData(
                        username = resultSet.getString("user_id"),
                        trainingName = resultSet.getString("training_name"),
                        trainingDate = resultSet.getTimestamp("training_date").toString(),
                        trainingDuration = resultSet.getInt("training_duration"),
                        avgHeartRate = resultSet.getDouble("avg_heart_rate"),
                        maxHeartRate = resultSet.getInt("max_heart_rate"),
                        caloriesBurned = resultSet.getDouble("calories_burned"),
                        met = resultSet.getDouble("MET"),
                        intensityZones = intensityZones,
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
