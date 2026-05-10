package swm.calender.calendar.domain.model

import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.time.DateTimeRange
import swm.calender.core.enums.AvailabilitySource
import java.time.Instant

data class MentoringSchedule(
    val id: Long? = null,
    val teamId: TeamId,
    val externalSourceId: String,
    val title: String,
    val range: DateTimeRange,
    val location: String? = null,
    val description: String? = null,
    val googleEventId: String? = null,
    val createdAt: Instant,
) {
    init {
        validateExternalSourceId(externalSourceId)
        validateTitle(title)
    }

    fun registerGoogleEvent(googleEventId: String): MentoringSchedule {
        if (googleEventId.isBlank()) {
            throw CalendarDomainException(CalendarErrorMessage.GOOGLE_EVENT_ID_REQUIRED)
        }

        return copy(googleEventId = googleEventId.trim())
    }

    fun toBusyAvailabilitySlot(): AvailabilitySlot {
        return AvailabilitySlot(
            teamId = teamId,
            source = AvailabilitySource.GOOGLE_CALENDAR,
            range = range,
            availableMemberCount = 0,
            busyMemberCount = 1,
            createdAt = createdAt,
        )
    }

    companion object {
        fun create(
            teamId: TeamId,
            externalSourceId: String,
            title: String,
            range: DateTimeRange,
            location: String? = null,
            description: String? = null,
            googleEventId: String? = null,
            createdAt: Instant,
        ): MentoringSchedule {
            return MentoringSchedule(
                teamId = teamId,
                externalSourceId = externalSourceId.trim(),
                title = title.trim(),
                range = range,
                location = location?.trim()?.takeIf { it.isNotEmpty() },
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                googleEventId = googleEventId?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = createdAt,
            )
        }

        private fun validateExternalSourceId(externalSourceId: String) {
            if (externalSourceId.isBlank()) {
                throw CalendarDomainException(CalendarErrorMessage.MENTORING_SCHEDULE_EXTERNAL_SOURCE_ID_REQUIRED)
            }
        }

        private fun validateTitle(title: String) {
            if (title.isBlank()) {
                throw CalendarDomainException(CalendarErrorMessage.MENTORING_SCHEDULE_TITLE_REQUIRED)
            }
        }
    }
}
