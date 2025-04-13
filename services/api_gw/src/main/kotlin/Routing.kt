package com.example

import com.example.data.*
import com.example.util.UUIDWrapper
import com.example.util.uuidEquals
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import keydb.sendEvent
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.*

fun Application.configureRouting() {
    routing {
        get("/echo/{phrase}") {
            val result = getResultFromMicroservice("channel", resultCondition = { true }) {
                sendEvent("echo", call.parameters["phrase"]!!)
            }
            call.respond(result)
        }

        post("/login") {
            val uuidWrapper = UUIDWrapper(UUID.randomUUID(), Json.decodeFromString<LoginDto>(call.receiveText()))
            val result = getResultFromMicroservice(
                "auth:response:Login", "error", resultCondition = uuidEquals(uuidWrapper.uuid)
            ) {
                sendEvent("auth:request:Login", Json.encodeToString(uuidWrapper))
            }
            call.respond(result)
        }

        post("/register") {
            val uuidWrapper = UUIDWrapper(UUID.randomUUID(), Json.decodeFromString<RegisterDto>(call.receiveText()))
            val result = getResultFromMicroservice(
                "auth:response:Register", "error", resultCondition = uuidEquals(uuidWrapper.uuid)
            ) {
                sendEvent("auth:request:Register", Json.encodeToString(uuidWrapper))
            }
            call.respond(result)
        }

        authenticate("auth-bearer") {
            post("/registerDevice") {
                val name = getLogin()
                val phoneId = call.receiveText()
                val request = NotifyRegisterPhone(name, phoneId)
                sendEvent("notify:request:registerDevice", Json.encodeToString(request))
            }

            post("/logout") {
                val tokenDto = TokenDto(UUID.randomUUID(), call.request.authorization()!!.substringAfter("Bearer "))
                val result = getResultFromMicroservice(
                    "auth:response:JwtRevoke", "error", resultCondition = uuidEquals(tokenDto.uuid)
                ) {
                    sendEvent("auth:request:JwtRevoke", Json.encodeToString(tokenDto))
                }
                call.respond(result)
            }

            get("/getUserInfo") {
                val dto = UserDataRequest(UUID.randomUUID(), getLogin())
                println(dto)
                val result =
                    getResultFromMicroservice("user_data:response:UserData", resultCondition = uuidEquals(dto.uuid)) {
                        sendEvent("user_data:request:UserData", Json.encodeToString(dto))
                    }
                call.respond(result)
            }

            post("/updateInfo") {
                val payload = Json.decodeFromString<UserDataDTO>(call.receiveText()).copy(username = getLogin())
                val uuidWrapper = UUIDWrapper(UUID.randomUUID(), payload)
                sendEvent("user_data:request:UpdateUserData", Json.encodeToString(uuidWrapper))
                call.respond(HttpStatusCode.Created)
            }

            get("/getUserInfo/{username}") {
                val username = call.parameters["username"]!!
                val request = UUIDWrapper(UUID.randomUUID(), username)
                val result = getResultFromMicroservice(
                    "social:response:GetUserProfile", "error", resultCondition = uuidEquals(request.uuid)
                ) {
                    sendEvent("social:request:GetUserProfile", Json.encodeToString(request))
                }
                call.respond(result)
            }

            get("/getFriends") {
                val login = getLogin()
                val request = UUIDWrapper(UUID.randomUUID(), login)
                val result = getResultFromMicroservice(
                    "social:response:GetFriendsList", "error", resultCondition = uuidEquals(request.uuid)
                ) {
                    sendEvent("social:request:GetFriendsList", Json.encodeToString(request))
                }
                call.respond(result)
            }

            get("/getAchievements") {
                TODO()
            }

            get("/getAchievements/{username}") {
                TODO()
            }

            get("/getFriendsRequests") {
                val username = getLogin()
                val request = UUIDWrapper(UUID.randomUUID(), username)
                val result = getResultFromMicroservice(
                    "social:response:GetFriendsRequests", "error", resultCondition = uuidEquals(request.uuid)
                ) {
                    sendEvent("social:request:GetFriendsRequests", Json.encodeToString(request))
                }

                call.respond(result)
            }

            post("/addFriend/{username}") {
                val from = getLogin()
                val to = call.parameters["username"]!!

                val request = UUIDWrapper(UUID.randomUUID(), FriendshipRequestDTO(from, to))
                sendEvent("social:request:ProposeFriendship", Json.encodeToString(request))

                call.respond(HttpStatusCode.Created)
            }

            post("/friendRequestAnswer/accept/{username}") {
                val from = getLogin()
                val to = call.parameters["username"]!!

                val request = UUIDWrapper(UUID.randomUUID(), FriendshipResponseDTO(from, to, "accept"))
                sendEvent("social:request:RespondToFriendship", Json.encodeToString(request))

                call.respond(HttpStatusCode.Created)
            }

            post("/friendRequestAnswer/reject/{username}") {
                val from = getLogin()
                val to = call.parameters["username"]!!

                val request = UUIDWrapper(UUID.randomUUID(), FriendshipResponseDTO(from, to, "reject"))
                sendEvent("social:request:RespondToFriendship", Json.encodeToString(request))

                call.respond(HttpStatusCode.Created)
            }

            get("/getWeightHistory") {
                val name = getLogin()

                val request = UUIDWrapper(
                    UUID.randomUUID(), APIGatewayToWeightHistoryRequest(
                        username = name, weightControlWish = null
                    )
                )
                val result = getResultFromMicroservice(
                    "weight_history:response:WeightHistoryAndPrediction", resultCondition = uuidEquals(request.uuid)
                ) {
                    sendEvent("weight_history:request:WeightHistoryAndPrediction", Json.encodeToString(request))
                }

                call.respond(result)
            }

            get("/getActivities") {
                val user = getLogin()
                val activity = APIGatewayToActivityRequest(user)
                val request = UUIDWrapper(UUID.randomUUID(), activity)
                val result = getResultFromMicroservice(
                    "activity:response:GetAllTrainings", resultCondition = uuidEquals(request.uuid)
                ) {
                    sendEvent("activity:request:GetAllTrainings", Json.encodeToString(request))
                }
                call.respond(result)
            }

            post("/newActivity") {
                val user = getLogin()
                val data = Json.decodeFromString<ActivityDTO>(call.receiveText())
                val activity = APIGatewayToActivityRequest(user, jsonWorkout = Json.encodeToString(data))
                val request = UUIDWrapper(UUID.randomUUID(), activity)
                val result = getResultFromMicroservice(
                    "activity:response:AddTraining", resultCondition = uuidEquals(request.uuid)
                ) {
                    sendEvent("activity:request:AddTraining", Json.encodeToString(request))
                }
                call.respond(result)
            }

            get("/getNutritionMenu") {
                val dto = RationRequestDTO(
                    id = UUID.randomUUID(), login = getLogin()
                )
                val result =
                    getResultFromMicroservice("nutrition:response:today_ration", resultCondition = uuidEquals(dto.id)) {
                        sendEvent("nutrition:request:today_ration", Json.encodeToString(dto))
                    }
                call.respond(result)
            }

            get("/getNutritionMenu/{date}") {
                val date = call.parameters["date"]!!.toLocalDate()
                val dto = HistoryRequestRationByDateDTO(
                    id = UUID.randomUUID(), login = getLogin(), date = date
                )
                val result =
                    getResultFromMicroservice("nutrition:response:today_ration", resultCondition = uuidEquals(dto.id)) {
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
    val result = getResultFromMicroservice(responseChannelName, resultCondition = uuidEquals(uuidWrapper.uuid)) {
        sendEvent(requestChannelName, Json.encodeToString(uuidWrapper))
    }
    return result
}
