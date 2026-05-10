package swm.calender.storage.db.core.calendar

import swm.calender.calendar.domain.model.AvailabilitySlot
import swm.calender.calendar.domain.model.MentoringSchedule
import swm.calender.calendar.domain.model.TeamCalendar
import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.common.time.DateTimeRange
import swm.calender.core.enums.AvailabilitySource
import swm.calender.core.enums.CalendarStatus
import swm.calender.core.enums.When2meetLinkStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal data class TeamCalendarEntity(
    val id: Long,
    val teamId: Long,
    val googleCalendarId: String?,
    val activatedByUserId: Long,
    val status: CalendarStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): TeamCalendar {
        return TeamCalendar(
            id = id,
            teamId = TeamId(teamId),
            googleCalendarId = googleCalendarId,
            activatedByUserId = UserId(activatedByUserId),
            status = status,
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
        )
    }
}

internal data class MentoringScheduleEntity(
    val id: Long,
    val teamId: Long,
    val externalSourceId: String,
    val title: String,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val location: String?,
    val description: String?,
    val googleEventId: String?,
    val createdAt: LocalDateTime,
) {
    fun toDomain(): MentoringSchedule {
        return MentoringSchedule(
            id = id,
            teamId = TeamId(teamId),
            externalSourceId = externalSourceId,
            title = title,
            range = DateTimeRange(
                startsAt = startsAt.toOffsetDateTime(),
                endsAt = endsAt.toOffsetDateTime(),
            ),
            location = location,
            description = description,
            googleEventId = googleEventId,
            createdAt = createdAt.toInstant(),
        )
    }
}

internal data class When2meetLinkEntity(
    val id: Long,
    val teamId: Long,
    val url: String,
    val status: When2meetLinkStatus,
    val failureReason: String?,
    val lastParsedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    fun toDomain(): When2meetLink {
        return When2meetLink(
            id = id,
            teamId = TeamId(teamId),
            url = url,
            status = status,
            failureReason = failureReason,
            lastParsedAt = lastParsedAt?.toInstant(),
            createdAt = createdAt.toInstant(),
            updatedAt = updatedAt.toInstant(),
        )
    }
}

internal data class AvailabilitySlotEntity(
    val id: Long,
    val teamId: Long,
    val source: AvailabilitySource,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val availableMemberCount: Int,
    val busyMemberCount: Int,
    val createdAt: LocalDateTime,
) {
    fun toDomain(): AvailabilitySlot {
        return AvailabilitySlot(
            id = id,
            teamId = TeamId(teamId),
            source = source,
            range = DateTimeRange(
                startsAt = startsAt.toOffsetDateTime(),
                endsAt = endsAt.toOffsetDateTime(),
            ),
            availableMemberCount = availableMemberCount,
            busyMemberCount = busyMemberCount,
            createdAt = createdAt.toInstant(),
        )
    }
}

internal fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)

internal fun OffsetDateTime.toUtcLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(toInstant(), ZoneOffset.UTC)

private fun LocalDateTime.toInstant(): Instant = toInstant(ZoneOffset.UTC)

private fun LocalDateTime.toOffsetDateTime(): OffsetDateTime = atOffset(ZoneOffset.UTC)
