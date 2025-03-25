package com.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import keydb.sendEvent
import keydb.subscribeChannel
import keydb.subscribeChannelWithUnsubscribe
import kotlinx.coroutines.cancel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun Application.configureRouting() {/*install(RequestValidation) {
        validate<String> { bodyText ->
            if (!bodyText.startsWith("Hello"))
                ValidationResult.Invalid("Body text should start with 'Hello'")
            else ValidationResult.Valid
        }
    }*/
    routing {
        get("/getWeightHistory") {
            val result = suspendCoroutine { continuation ->
                subscribeChannel("") { it ->
                    continuation.resume(it)
                }
            }
        }

        get("/echo/{phrase}") {
            val result = getResultFromMicroservice("channel", { true }) {
                sendEvent("echo", call.parameters["phrase"]!!)
            }
            call.respond(result)
        }
    }
}
