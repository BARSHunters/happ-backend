package com.example

import com.example.data.HistoryRequestRationByDateDTO
import com.example.data.RationRequestDTO
import com.example.data.UserDataRequest
import com.example.util.UUIDWrapper
import com.example.util.uuidEquals
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import keydb.sendEvent
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.*

fun Application.configureRouting() {/*install(RequestValidation) {
        validate<String> { bodyText ->
            if (!bodyText.startsWith("Hello"))
                ValidationResult.Invalid("Body text should start with 'Hello'")
            else ValidationResult.Valid
        }
    }*/
    routing {
        get("/echo/{phrase}") {
            val result = getResultFromMicroservice("channel", { true }) {
                sendEvent("echo", call.parameters["phrase"]!!)
                println("event sent")
            }
            call.respond(result)
        }

        post("/login") {
            val uuidWrapper = UUIDWrapper(UUID.randomUUID(), call.receiveText())
            val result = getResultFromMicroservice("auth:response:Login", uuidEquals(uuidWrapper.uuid)) {
                sendEvent("auth:request:Login", Json.encodeToString(uuidWrapper))
            }
            call.respond(result)
        }

        post("/register") {
            call.respond(wrapUUIDAndGetResult("auth:request:Register", "auth:response:Register", call.receiveText()))
        }

        authenticate("auth-bearer") {
            post("/logout") {
                TODO()
                // call.respond(wrapUUIDAndGetResult("auth:request:JwtRevoke", "auth:response:JwtRevoke", TODO()))
            }

            get("/getUserInfo") {
                val dto = UserDataRequest(UUID.randomUUID(), getLogin())
                val result = getResultFromMicroservice("user_data:response:UserData", uuidEquals(dto.uuid)) {
                    sendEvent("user_data:request:UserData", Json.encodeToString(dto))
                }
                call.respond(result)
            }

            get("/getUserInfo/{username}") {

            }

            post("/updateInfo") {
                val uuidWrapper = UUIDWrapper(UUID.randomUUID(), call.receiveText())
                sendEvent("user_data:request:UpdateUserData", Json.encodeToString(uuidWrapper))
            }

            get("/getFriends") {

            }

            get("/getAchievements") {

            }

            get("/getAchievements/{username}") {

            }

            get("/getFriendsRequests") {

            }

            post("/addFriend/{username}") {

            }

            post("/friendRequestAnswer/{username}") {

            }

            get("/getWeightHistory") {

            }

            get("/getActivities") {

            }

            post("/newActivity") {

            }

            get("/getNutritionMenu") {
                val dto = RationRequestDTO(
                    id = UUID.randomUUID(),
                    login = getLogin()
                )
                val result = getResultFromMicroservice("nutrition:response:today_ration", uuidEquals(dto.id)) {
                    sendEvent("nutrition:request:today_ration", Json.encodeToString(dto))
                }
                call.respond(result)
            }

            get("/getNutritionMenu/{date}") {
                val date = call.parameters["date"]!!.toLocalDate()
                val dto = HistoryRequestRationByDateDTO(
                    id = UUID.randomUUID(),
                    login = getLogin(),
                    date = date
                )
                val result = getResultFromMicroservice("nutrition:response:today_ration", uuidEquals(dto.id)) {
                    sendEvent("nutrition:request:today_ration", Json.encodeToString(dto))
                }
                call.respond(result)
            }

            get("/getNutritionMenus") {
                TODO()
            }
        }
    }
}

fun RoutingContext.getLogin() = call.principal<UserIdPrincipal>()?.name!!

fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

suspend fun wrapUUIDAndGetResult(requestChannelName: String, responseChannelName: String, request: String): String {
    val uuidWrapper = UUIDWrapper(UUID.randomUUID(), request)
    val result = getResultFromMicroservice(responseChannelName, uuidEquals(uuidWrapper.uuid)) {
        sendEvent(requestChannelName, Json.encodeToString(uuidWrapper))
    }
    return result
}
