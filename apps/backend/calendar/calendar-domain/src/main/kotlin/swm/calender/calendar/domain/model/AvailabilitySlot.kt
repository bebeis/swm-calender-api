package swm.calender.calendar.domain.model

import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.time.DateTimeRange
import swm.calender.core.enums.AvailabilitySource
import java.time.Instant

data class AvailabilitySlot(
    val id: Long? = null,
    val teamId: TeamId,
    val source: AvailabilitySource,
    val range: DateTimeRange,
    val availableMemberCount: Int,
    val busyMemberCount: Int,
    val createdAt: Instant,
) {
    init {
        if (availableMemberCount < 0 || busyMemberCount < 0) {
            throw CalendarDomainException(CalendarErrorMessage.AVAILABILITY_COUNT_NEGATIVE)
        }
    }

    fun overlaps(range: DateTimeRange): Boolean {
        return this.range.overlaps(range)
    }
}
