package swm.calender.storage.db.core.calendar

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import swm.calender.calendar.domain.CalendarRepository
import swm.calender.calendar.domain.model.AvailabilitySlot
import swm.calender.calendar.domain.model.MentoringSchedule
import swm.calender.calendar.domain.model.TeamCalendar
import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.time.DateTimeRange

@Repository
class CalendarExposedRepository : CalendarRepository {
    override fun saveTeamCalendar(teamCalendar: TeamCalendar): TeamCalendar {
        val rowId = resolveTeamCalendarRowId(
            id = teamCalendar.id,
            teamId = teamCalendar.teamId,
        )

        if (rowId == null) {
            TeamCalendarTable.insert {
                it[teamId] = teamCalendar.teamId.value
                it[googleCalendarId] = teamCalendar.googleCalendarId
                it[activatedByUserId] = teamCalendar.activatedByUserId.value
                it[status] = teamCalendar.status
                it[createdAt] = teamCalendar.createdAt.toLocalDateTime()
                it[updatedAt] = teamCalendar.updatedAt.toLocalDateTime()
            }
        } else {
            TeamCalendarTable.update(
                where = { TeamCalendarTable.id eq rowId },
            ) {
                it[teamId] = teamCalendar.teamId.value
                it[googleCalendarId] = teamCalendar.googleCalendarId
                it[activatedByUserId] = teamCalendar.activatedByUserId.value
                it[status] = teamCalendar.status
                it[createdAt] = teamCalendar.createdAt.toLocalDateTime()
                it[updatedAt] = teamCalendar.updatedAt.toLocalDateTime()
            }
        }

        return requireNotNull(findTeamCalendarByTeamIdInternal(teamCalendar.teamId))
    }

    override fun findTeamCalendarByTeamId(teamId: TeamId): TeamCalendar? {
        return findTeamCalendarByTeamIdInternal(teamId)
    }

    override fun saveMentoringSchedules(schedules: List<MentoringSchedule>): List<MentoringSchedule> {
        val savedIds = schedules.map { schedule ->
            MentoringScheduleTable.insert {
                it[teamId] = schedule.teamId.value
                it[externalSourceId] = schedule.externalSourceId
                it[title] = schedule.title
                it[startsAt] = schedule.range.startsAt.toUtcLocalDateTime()
                it[endsAt] = schedule.range.endsAt.toUtcLocalDateTime()
                it[location] = schedule.location
                it[description] = schedule.description
                it[googleEventId] = schedule.googleEventId
                it[createdAt] = schedule.createdAt.toLocalDateTime()
            }[MentoringScheduleTable.id]
        }

        return findMentoringSchedulesByIds(savedIds)
    }

    override fun findMentoringSchedulesByExternalSourceIds(
        teamId: TeamId,
        externalSourceIds: Collection<String>,
    ): List<MentoringSchedule> {
        val normalizedSourceIds = externalSourceIds
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        if (normalizedSourceIds.isEmpty()) {
            return emptyList()
        }

        return MentoringScheduleTable
            .selectAll()
            .where {
                (MentoringScheduleTable.teamId eq teamId.value) and
                    (MentoringScheduleTable.externalSourceId inList normalizedSourceIds)
            }
            .orderBy(MentoringScheduleTable.id)
            .map { it.toMentoringScheduleEntity().toDomain() }
    }

    override fun findMentoringSchedulesByTeamIdAndRange(
        teamId: TeamId,
        range: DateTimeRange,
    ): List<MentoringSchedule> {
        return MentoringScheduleTable
            .selectAll()
            .where {
                (MentoringScheduleTable.teamId eq teamId.value) and overlapCondition(
                    startsAtColumn = MentoringScheduleTable.startsAt,
                    endsAtColumn = MentoringScheduleTable.endsAt,
                    range = range,
                )
            }
            .orderBy(
                MentoringScheduleTable.startsAt to SortOrder.ASC,
                MentoringScheduleTable.endsAt to SortOrder.ASC,
                MentoringScheduleTable.id to SortOrder.ASC,
            )
            .map { it.toMentoringScheduleEntity().toDomain() }
    }

    override fun saveWhen2meetLink(when2meetLink: When2meetLink): When2meetLink {
        val rowId = resolveWhen2meetLinkRowId(
            id = when2meetLink.id,
            teamId = when2meetLink.teamId,
        )

        if (rowId == null) {
            When2meetLinkTable.insert {
                it[teamId] = when2meetLink.teamId.value
                it[url] = when2meetLink.url
                it[status] = when2meetLink.status
                it[failureReason] = when2meetLink.failureReason
                it[lastParsedAt] = when2meetLink.lastParsedAt?.toLocalDateTime()
                it[createdAt] = when2meetLink.createdAt.toLocalDateTime()
                it[updatedAt] = when2meetLink.updatedAt.toLocalDateTime()
            }
        } else {
            When2meetLinkTable.update(
                where = { When2meetLinkTable.id eq rowId },
            ) {
                it[teamId] = when2meetLink.teamId.value
                it[url] = when2meetLink.url
                it[status] = when2meetLink.status
                it[failureReason] = when2meetLink.failureReason
                it[lastParsedAt] = when2meetLink.lastParsedAt?.toLocalDateTime()
                it[createdAt] = when2meetLink.createdAt.toLocalDateTime()
                it[updatedAt] = when2meetLink.updatedAt.toLocalDateTime()
            }
        }

        return requireNotNull(findWhen2meetLinkByTeamIdInternal(when2meetLink.teamId))
    }

    override fun findWhen2meetLinkByTeamId(teamId: TeamId): When2meetLink? {
        return findWhen2meetLinkByTeamIdInternal(teamId)
    }

    override fun findAvailabilitySlotsByTeamIdAndRange(
        teamId: TeamId,
        range: DateTimeRange,
    ): List<AvailabilitySlot> {
        return AvailabilitySlotTable
            .selectAll()
            .where {
                (AvailabilitySlotTable.teamId eq teamId.value) and overlapCondition(
                    startsAtColumn = AvailabilitySlotTable.startsAt,
                    endsAtColumn = AvailabilitySlotTable.endsAt,
                    range = range,
                )
            }
            .orderBy(
                AvailabilitySlotTable.startsAt to SortOrder.ASC,
                AvailabilitySlotTable.endsAt to SortOrder.ASC,
                AvailabilitySlotTable.id to SortOrder.ASC,
            )
            .map { it.toAvailabilitySlotEntity().toDomain() }
    }

    private fun findTeamCalendarByTeamIdInternal(teamId: TeamId): TeamCalendar? {
        return TeamCalendarTable
            .selectAll()
            .where { TeamCalendarTable.teamId eq teamId.value }
            .singleOrNull()
            ?.toTeamCalendarEntity()
            ?.toDomain()
    }

    private fun findMentoringSchedulesByIds(ids: List<Long>): List<MentoringSchedule> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        val schedulesById = MentoringScheduleTable
            .selectAll()
            .where { MentoringScheduleTable.id inList ids }
            .map { it.toMentoringScheduleEntity().toDomain() }
            .associateBy { requireNotNull(it.id) }

        return ids.mapNotNull(schedulesById::get)
    }

    private fun findWhen2meetLinkByTeamIdInternal(teamId: TeamId): When2meetLink? {
        return When2meetLinkTable
            .selectAll()
            .where { When2meetLinkTable.teamId eq teamId.value }
            .singleOrNull()
            ?.toWhen2meetLinkEntity()
            ?.toDomain()
    }

    private fun resolveTeamCalendarRowId(
        id: Long?,
        teamId: TeamId,
    ): Long? {
        return id?.takeIf { teamCalendarRowExists(it) }
            ?: TeamCalendarTable
                .selectAll()
                .where { TeamCalendarTable.teamId eq teamId.value }
                .singleOrNull()
                ?.get(TeamCalendarTable.id)
    }

    private fun resolveWhen2meetLinkRowId(
        id: Long?,
        teamId: TeamId,
    ): Long? {
        return id?.takeIf { when2meetLinkRowExists(it) }
            ?: When2meetLinkTable
                .selectAll()
                .where { When2meetLinkTable.teamId eq teamId.value }
                .singleOrNull()
                ?.get(When2meetLinkTable.id)
    }

    private fun teamCalendarRowExists(id: Long): Boolean {
        return TeamCalendarTable
            .selectAll()
            .where { TeamCalendarTable.id eq id }
            .limit(1)
            .any()
    }

    private fun when2meetLinkRowExists(id: Long): Boolean {
        return When2meetLinkTable
            .selectAll()
            .where { When2meetLinkTable.id eq id }
            .limit(1)
            .any()
    }

    private fun overlapCondition(
        startsAtColumn: org.jetbrains.exposed.v1.core.Column<java.time.LocalDateTime>,
        endsAtColumn: org.jetbrains.exposed.v1.core.Column<java.time.LocalDateTime>,
        range: DateTimeRange,
    ) = (startsAtColumn less range.endsAt.toUtcLocalDateTime()) and
        (endsAtColumn greater range.startsAt.toUtcLocalDateTime())
}
