package com.example

import com.example.data.TokenValidationRequest
import com.example.data.TokenValidationResponse
import com.example.util.uuidEquals
import io.ktor.server.application.*
import io.ktor.server.auth.*
import keydb.sendEvent
import kotlinx.serialization.json.Json
import java.util.UUID

fun Application.configureSecurity() {
    install(Authentication) {
        bearer("auth-bearer") {
            realm = "Access to the services"
            authenticate { tokenCredential ->
                /*if (tokenCredential.token == "abc123") {
                    UserIdPrincipal("jetbrains")
                } else {
                    null
                }*/

                val token = tokenCredential.token
                val uuid = UUID.randomUUID()

                val request = TokenValidationRequest(uuid, token)

                val validationResultRaw = getResultFromMicroservice("auth:response:JwtValidation", uuidEquals(uuid)) {
                    sendEvent("auth:request:JwtValidation", Json.encodeToString(request))
                }

                val validationResult = Json.decodeFromString<TokenValidationResponse>(validationResultRaw)

                when (validationResult.message) {
                    "valid" -> UserIdPrincipal(validationResult.name)
                    else -> null
                }
            }
        }
    }
}
