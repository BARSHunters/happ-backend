package model.response

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val notificationType: NotificationType,
    val notificationReceiver: String,
    val payload: NotificationPayload
)

enum class NotificationType {
    FRIEND_REQUEST
}

@Serializable
sealed class NotificationPayload {
    @Serializable
    class FriendRequestPayload(@Suppress("unused") val friendName: String) : NotificationPayload()
}
