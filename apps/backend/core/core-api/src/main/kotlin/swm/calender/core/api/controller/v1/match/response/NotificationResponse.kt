package swm.calender.core.api.controller.v1.match.response

import swm.calender.core.api.controller.v1.match.service.NotificationSnapshot
import swm.calender.core.enums.NotificationType
import java.time.Instant

data class NotificationListResponse(
    val items: List<NotificationResponse>,
) {
    companion object {
        fun from(snapshots: List<NotificationSnapshot>): NotificationListResponse {
            return NotificationListResponse(
                items = snapshots.map(NotificationResponse::from),
            )
        }
    }
}

data class NotificationResponse(
    val notificationId: Long,
    val type: NotificationType,
    val message: String,
    val read: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(snapshot: NotificationSnapshot): NotificationResponse {
            return NotificationResponse(
                notificationId = snapshot.notificationId,
                type = snapshot.type,
                message = snapshot.message,
                read = snapshot.read,
                createdAt = snapshot.createdAt,
            )
        }
    }
}
