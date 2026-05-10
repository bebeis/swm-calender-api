package swm.calender.calendar.service.request

import swm.calender.core.common.id.UserId
import swm.calender.core.common.time.DateTimeRange
import java.time.OffsetDateTime

data class CalendarUnifiedAvailabilityRequest(
    val actorUserId: UserId,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
) {
    fun toRange(): DateTimeRange {
        return DateTimeRange(
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }
}
