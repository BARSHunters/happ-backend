package com.example

import keydb.subscribeChannelWithUnsubscribe
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun getResultFromMicroservice(
    channelName: String,
    resultCondition: (String) -> Boolean,
    eventSender: () -> Unit,
) = coroutineScope {
    suspendCoroutine { continuation ->
        subscribeChannelWithUnsubscribe(channelName) { message, unsubscribe ->
            if (resultCondition(message)) {
                continuation.resume(message)
                unsubscribe()
            }
        }

        eventSender()
    }
}