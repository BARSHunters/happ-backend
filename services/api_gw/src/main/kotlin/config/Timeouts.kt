package com.example.config

import io.ktor.server.application.*
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.util.*
import kotlinx.coroutines.withTimeout

class Timeouts {
    class Config(var requestTimeout: Long = 5000L)

    companion object : BaseApplicationPlugin<ApplicationCallPipeline, Config, Unit> {
        override val key: AttributeKey<Unit> = AttributeKey("Timeouts")

        override fun install(pipeline: ApplicationCallPipeline, configure: Config.() -> Unit) {
            val requestTimeout = Config().apply(configure).requestTimeout
            if (requestTimeout <= 0) return

            pipeline.intercept(Plugins) {
                withTimeout(requestTimeout) {
                    proceed()
                }
            }
        }
    }
}