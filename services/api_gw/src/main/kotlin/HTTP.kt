package com.example

import com.example.config.Timeouts
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureHTTP() {
    install(Timeouts)

    routing {
        swaggerUI(path = "openapi")
    }
}
