package swm.calender.match.service.response

import swm.calender.core.enums.NotificationType
import swm.calender.match.domain.model.Notification
import java.time.Instant

data class NotificationResponse(
    val notificationId: Long,
    val type: NotificationType,
    val message: String,
    val read: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse {
            return NotificationResponse(
                notificationId = requireNotNull(notification.id),
                type = notification.type,
                message = notification.message,
                read = notification.readAt != null,
                createdAt = notification.createdAt,
            )
        }
    }
}
