package com.example.test

import com.example.test.models.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

data class RequestData(val requestIndex: Long, val responseTimeMs: Long)

class DataCollector {
    private val requestData = mutableListOf<RequestData>()
    private val requestCounter = AtomicLong(0)

    fun addRequestData(responseTimeMs: Long) {
        synchronized(requestData) {
            requestData.add(RequestData(requestCounter.incrementAndGet(), responseTimeMs))
        }
    }

    fun exportToCsv(filename: String = "request_metrics.csv") {
        val csvContent = StringBuilder().apply {
            append("RequestIndex,ResponseTimeMs\n")
            synchronized(requestData) {
                requestData.forEach { data ->
                    append("${data.requestIndex},${data.responseTimeMs}\n")
                }
            }
        }
        File(filename).writeText(csvContent.toString())
    }
}

class TestClient(
    private val id: Int,
    private val httpClient: HttpClient,
    private var token: String? = null,
    private val dataCollector: DataCollector
) {
    companion object {
        private const val BASEURL = "http://localhost:8080"
        private val random = Random(System.currentTimeMillis())
        private val logger = LoggerFactory.getLogger(TestClient::class.java)
    }

    private suspend fun makeRequest(
        method: HttpMethod,
        endpoint: String,
        body: Any? = null,
        headers: Headers? = null
    ): HttpResponse {
        delay(random.nextLong(500, 1000))
        val startTime = System.currentTimeMillis()
        return try {
            val response = httpClient.request("$BASEURL/$endpoint") {
                this.method = method
                contentType(ContentType.Application.Json)
                if (body != null) {
                    setBody(body)
                }
                headers?.let {
                    it.forEach { name, values ->
                        this.headers.appendAll(name, values)
                    }
                }
            }
            val responseTime = System.currentTimeMillis() - startTime
            dataCollector.addRequestData(responseTime)
            response
        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            dataCollector.addRequestData(responseTime)
            throw e
        }
    }

    private suspend fun register(): Boolean {
        val registerDto = RegisterDto(
            username = "username_$id",
            password = "password",
            name = "User",
            birthDate = "1990-01-01",
            gender = "MALE",
            heightCm = random.nextInt(150, 201),
            weightKg = random.nextFloat() * 50 + 50,
            weightDesire = "GAIN"
        )
        val requestBody = Json.encodeToString(RegisterDto.serializer(), registerDto)
        logger.info("Client $id - Register request body: $requestBody")

        return try {
            val response = makeRequest(HttpMethod.Post, "register", registerDto)
            if (response.status == HttpStatusCode.OK) {
                val responseBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
                token = responseBody["jwt"]
                logger.info("Client $id - Registered successfully")
                true
            } else {
                logger.error("Client $id - Registration failed with status: ${response.status}, body: ${response.bodyAsText()}")
                false
            }
        } catch (e: Exception) {
            logger.error("Client $id - Registration failed: ${e.message}")
            false
        }
    }

    private suspend fun login(): Boolean {
        val loginDto = LoginDto(username = "username_$id", password = "password")
        val requestBody = Json.encodeToString(LoginDto.serializer(), loginDto)
        logger.info("Client $id - Login request body: $requestBody")

        return try {
            val response = makeRequest(HttpMethod.Post, "login", loginDto)
            if (response.status == HttpStatusCode.OK) {
                val loginResponse = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
                token = loginResponse["jwt"]
                true
            } else {
                logger.error("Client $id - Login failed with status: ${response.status}, body: ${response.bodyAsText()}")
                false
            }
        } catch (e: Exception) {
            logger.error("Client $id - Login failed: ${e.message}")
            false
        }
    }

    private suspend fun addActivity(): Boolean {
        val heartRates = ArrayList<HeartRate>().apply {
            add(HeartRate(timestamp = 1700000000, heartRate = 120))
        }
        val activityDto = ActivityDTO(duration = "00:45:01", heartRates = heartRates)
        val requestBody = Json.encodeToString(ActivityDTO.serializer(), activityDto)
        logger.info("Client $id - Add activity request body: $requestBody")

        return try {
            val response = makeRequest(
                HttpMethod.Post,
                "newActivity",
                activityDto,
                headers = headersOf(HttpHeaders.Authorization, "Bearer $token")
            )
            if (response.status == HttpStatusCode.OK) {
                logger.info("Client $id - Activity added successfully")
                true
            } else {
                logger.error("Client $id - Add activity failed with status: ${response.status}, body: ${response.bodyAsText()}")
                false
            }
        } catch (e: Exception) {
            logger.error("Client $id - Add activity failed: ${e.message}")
            false
        }
    }

    private suspend fun changeWeight(): Boolean {
        val userDataDto = UserDataDto(
            username = "username_$id",
            name = "User",
            birthDate = "1990-01-01",
            gender = "MALE",
            heightCm = random.nextInt(150, 201),
            weightKg = random.nextFloat() * 50 + 50,
            weightDesire = "REMAIN"
        )
        val requestBody = Json.encodeToString(UserDataDto.serializer(), userDataDto)
        logger.info("Client $id - Change weight request body: $requestBody")

        return try {
            val response = makeRequest(
                HttpMethod.Post,
                "updateInfo",
                userDataDto,
                headers = headersOf(HttpHeaders.Authorization, "Bearer $token")
            )
            if (response.status == HttpStatusCode.OK) {
                logger.info("Client $id - Weight changed successfully")
                true
            } else {
                logger.error("Client $id - Change weight failed with status: ${response.status}, body: ${response.bodyAsText()}")
                false
            }
        } catch (e: Exception) {
            logger.error("Client $id - Change weight failed: ${e.message}")
            false
        }
    }

    suspend fun run() {
        if (register() || login()) {
            delay(random.nextLong(500, 2000))
            addActivity()
            delay(random.nextLong(500, 2000))
            changeWeight()
        } else {
            logger.info("Client $id - Both registration and login failed, skipping actions")
        }
    }
}

fun main() = runBlocking {
    // Настройка HTTP-клиента с пулом соединений
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        engine {
            endpoint {
                maxConnectionsPerRoute = 100
                maxConnectionsCount = 200
                connectTimeout = 10_000 // Тайм-аут подключения
                requestTimeout = 30_000 // Тайм-аут запроса
            }
        }
    }

    val dataCollector = DataCollector()
    val activeRequests = AtomicInteger(0) // Счетчик активных запросов для мониторинга

    try {
        val numberOfClients = 10000
        val batchSize = 10
        val batchDelay = 500L // Уменьшенная задержка между батчами для плавной нагрузки
        val maxConcurrentRequests = 50 // Максимум одновременных запросов

        val clients = List(numberOfClients) { TestClient(it, httpClient, dataCollector = dataCollector) }
        val random = Random(System.currentTimeMillis())

        // Функция для запуска батча клиентов с ограничением одновременных запросов
        suspend fun runBatch(chunk: List<TestClient>) {
            chunk.map { client ->
                launch(Dispatchers.IO) {
                    // Ожидание, если слишком много активных запросов
                    while (activeRequests.get() >= maxConcurrentRequests) {
                        delay(100)
                    }
                    activeRequests.incrementAndGet()
                    try {
                        client.run()
                    } catch (e: Exception) {
                        println("Error in client: ${e.message}")
                    } finally {
                        activeRequests.decrementAndGet()
                    }
                }
            }.joinAll()
        }

        // Первый этап: запуск всех клиентов по батчам
        clients.chunked(batchSize).forEach { chunk ->
            runBatch(chunk)
            delay(batchDelay)
        }

        // Второй этап: случайные запросы с контролируемой нагрузкой
        while (isActive) {
            val selectedClients = clients.shuffled(random).take(batchSize)
            runBatch(selectedClients)
            delay(random.nextLong(1000, 3000)) // Уменьшенная случайная задержка
        }
    } catch (e: CancellationException) {
        println("Test cancelled: ${e.message}")
    } catch (e: Exception) {
        println("Unexpected error: ${e.message}")
    } finally {
        dataCollector.exportToCsv()
        httpClient.close()
    }
}