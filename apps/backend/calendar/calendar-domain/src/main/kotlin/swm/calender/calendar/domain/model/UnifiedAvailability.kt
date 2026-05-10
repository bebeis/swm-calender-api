package swm.calender.calendar.domain.model

import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.time.DateTimeRange
import java.time.Instant

data class UnifiedAvailability(
    val teamId: TeamId,
    val range: DateTimeRange,
    val slots: List<UnifiedAvailabilitySlot>,
    val generatedAt: Instant,
)

data class UnifiedAvailabilitySlot(
    val range: DateTimeRange,
    val availableMemberCount: Int,
    val busyMemberCount: Int,
) {
    init {
        if (availableMemberCount < 0 || busyMemberCount < 0) {
            throw CalendarDomainException(CalendarErrorMessage.AVAILABILITY_COUNT_NEGATIVE)
        }
    }
}
