package swm.calender.calendar.domain.model

import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CalendarStatus
import java.time.Instant

data class TeamCalendar(
    val id: Long? = null,
    val teamId: TeamId,
    val googleCalendarId: String? = null,
    val activatedByUserId: UserId,
    val status: CalendarStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (status == CalendarStatus.ACTIVE && googleCalendarId.isNullOrBlank()) {
            throw CalendarDomainException(CalendarErrorMessage.TEAM_CALENDAR_ID_REQUIRED)
        }
    }

    fun activate(
        googleCalendarId: String,
        actorUserId: UserId,
        occurredAt: Instant,
    ): TeamCalendar {
        validateGoogleCalendarId(googleCalendarId)

        return copy(
            googleCalendarId = googleCalendarId.trim(),
            activatedByUserId = actorUserId,
            status = CalendarStatus.ACTIVE,
            updatedAt = occurredAt,
        )
    }

    fun markAuthRequired(
        actorUserId: UserId,
        occurredAt: Instant,
    ): TeamCalendar {
        return copy(
            activatedByUserId = actorUserId,
            status = CalendarStatus.AUTH_REQUIRED,
            updatedAt = occurredAt,
        )
    }

    fun disable(occurredAt: Instant): TeamCalendar {
        return copy(
            status = CalendarStatus.DISABLED,
            updatedAt = occurredAt,
        )
    }

    fun requireActive(): TeamCalendar {
        return when (status) {
            CalendarStatus.ACTIVE -> this
            CalendarStatus.AUTH_REQUIRED -> throw CalendarDomainException(
                CalendarErrorMessage.TEAM_CALENDAR_AUTH_REQUIRED,
            )
            CalendarStatus.DISABLED -> throw CalendarDomainException(
                CalendarErrorMessage.CALENDAR_SUB_SERVICE_DISABLED,
            )
        }
    }

    companion object {
        fun createAuthRequired(
            teamId: TeamId,
            actorUserId: UserId,
            createdAt: Instant,
        ): TeamCalendar {
            return TeamCalendar(
                teamId = teamId,
                activatedByUserId = actorUserId,
                status = CalendarStatus.AUTH_REQUIRED,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }

        private fun validateGoogleCalendarId(googleCalendarId: String) {
            if (googleCalendarId.isBlank()) {
                throw CalendarDomainException(CalendarErrorMessage.TEAM_CALENDAR_ID_REQUIRED)
            }
        }
    }
}
