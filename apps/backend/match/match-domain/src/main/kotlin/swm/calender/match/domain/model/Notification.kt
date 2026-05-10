package swm.calender.match.domain.model

import swm.calender.core.common.id.RequestId
import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.NotificationType
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant

enum class NotificationReferenceType {
    MATCH_REQUEST,
}

data class Notification(
    val id: Long? = null,
    val teamId: TeamId,
    val type: NotificationType,
    val referenceType: NotificationReferenceType,
    val referenceId: Long,
    val message: String,
    val readAt: Instant? = null,
    val createdAt: Instant,
) {
    init {
        if (message.isBlank()) {
            throw MatchDomainException(MatchErrorMessage.NOTIFICATION_MESSAGE_REQUIRED)
        }
    }

    companion object {
        fun requestReceived(
            teamId: TeamId,
            requestId: RequestId,
            createdAt: Instant,
        ): Notification {
            return requestNotification(
                teamId = teamId,
                type = NotificationType.REQUEST_RECEIVED,
                requestId = requestId,
                message = "New beta request received.",
                createdAt = createdAt,
            )
        }

        fun requestAccepted(
            teamId: TeamId,
            requestId: RequestId,
            createdAt: Instant,
        ): Notification {
            return requestNotification(
                teamId = teamId,
                type = NotificationType.REQUEST_ACCEPTED,
                requestId = requestId,
                message = "Beta request accepted.",
                createdAt = createdAt,
            )
        }

        fun requestRejected(
            teamId: TeamId,
            requestId: RequestId,
            createdAt: Instant,
        ): Notification {
            return requestNotification(
                teamId = teamId,
                type = NotificationType.REQUEST_REJECTED,
                requestId = requestId,
                message = "Beta request rejected.",
                createdAt = createdAt,
            )
        }

        fun requestCanceled(
            teamId: TeamId,
            requestId: RequestId,
            createdAt: Instant,
        ): Notification {
            return requestNotification(
                teamId = teamId,
                type = NotificationType.REQUEST_CANCELED,
                requestId = requestId,
                message = "Beta request canceled.",
                createdAt = createdAt,
            )
        }

        private fun requestNotification(
            teamId: TeamId,
            type: NotificationType,
            requestId: RequestId,
            message: String,
            createdAt: Instant,
        ): Notification {
            return Notification(
                teamId = teamId,
                type = type,
                referenceType = NotificationReferenceType.MATCH_REQUEST,
                referenceId = requestId.value,
                message = message,
                createdAt = createdAt,
            )
        }
    }
}
