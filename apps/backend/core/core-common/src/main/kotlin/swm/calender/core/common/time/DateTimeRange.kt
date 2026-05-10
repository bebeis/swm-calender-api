package swm.calender.core.common.time

import java.time.OffsetDateTime

data class DateTimeRange(
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
) {
    init {
        require(endsAt.isAfter(startsAt)) { "endsAt must be after startsAt." }
    }

    fun overlaps(other: DateTimeRange): Boolean {
        return startsAt.isBefore(other.endsAt) && endsAt.isAfter(other.startsAt)
    }
}
