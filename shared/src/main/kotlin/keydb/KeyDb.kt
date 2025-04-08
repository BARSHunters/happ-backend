package keydb

import kotlinx.coroutines.*
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub

fun CoroutineScope.subscribeChannelWithUnsubscribe(
    vararg channelNames: String,
    onMessage: (String, () -> Unit) -> Unit
) = launch(Dispatchers.IO) {
    Jedis("localhost", 6379).use {
        it.subscribe(object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) =
                runCatching { onMessage(message) { this.unsubscribe() } }
                    .onFailure(::handleFailure)
                    .getOrDefault(Unit)
        }, *channelNames)
    }
}

fun CoroutineScope.subscribeChannel(channelName: String, onMessage: (String) -> Unit) = launch(Dispatchers.IO) {
    Jedis("localhost", 6379).use {
        it.subscribe(object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) = runCatching { onMessage(message) }
                .onFailure(::handleFailure)
                .getOrDefault(Unit)
        }, channelName)
    }
}

fun runServiceListener(
    channels: Map<String, (String) -> Unit>, main: () -> Unit = { }
): Nothing = runBlocking {
    for (channel in channels) {
        subscribeChannel(channel.key, channel.value)
    }

    main()

    awaitCancellation()
}

fun sendEvent(channel: String, message: String) {
    Jedis("localhost", 6379).use {
        it.publish(channel, message)
    }
}
