package swm.calender.core.api.controller.v1.match.service

import org.springframework.stereotype.Component
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.enums.NotificationType
import swm.calender.match.service.NotificationService
import java.time.Instant
import swm.calender.match.service.response.NotificationResponse as NotificationServiceResponse

@Component
class NotificationApiFacade(
    private val notificationService: NotificationService,
) {
    fun listNotifications(user: AuthenticatedUser): List<NotificationSnapshot> {
        return notificationService.listNotifications(user.userId)
            .map(NotificationSnapshot::from)
    }
}

data class NotificationSnapshot(
    val notificationId: Long,
    val type: NotificationType,
    val message: String,
    val read: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(response: NotificationServiceResponse): NotificationSnapshot {
            return NotificationSnapshot(
                notificationId = response.notificationId,
                type = response.type,
                message = response.message,
                read = response.read,
                createdAt = response.createdAt,
            )
        }
    }
}
