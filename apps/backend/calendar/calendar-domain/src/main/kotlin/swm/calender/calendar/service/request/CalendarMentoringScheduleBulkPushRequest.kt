package swm.calender.calendar.service.request

import swm.calender.core.common.id.UserId
import swm.calender.core.common.time.DateTimeRange
import java.time.OffsetDateTime

data class CalendarMentoringScheduleBulkPushRequest(
    val actorUserId: UserId,
    val schedules: List<CalendarMentoringScheduleRequestItem>,
)

data class CalendarMentoringScheduleRequestItem(
    val externalSourceId: String,
    val title: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val location: String? = null,
    val description: String? = null,
) {
    fun toRange(): DateTimeRange {
        return DateTimeRange(
            startsAt = startsAt,
            endsAt = endsAt,
        )
    }
}
