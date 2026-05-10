package swm.calender.storage.db.core.calendar

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import swm.calender.core.enums.AvailabilitySource
import swm.calender.core.enums.CalendarStatus
import swm.calender.core.enums.When2meetLinkStatus
import swm.calender.storage.db.core.team.TeamTable

object TeamCalendarTable : Table("team_calendar") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id).uniqueIndex()
    val googleCalendarId = varchar("google_calendar_id", 255).nullable()
    val activatedByUserId = long("activated_by_user_id")
    val status = enumerationByName<CalendarStatus>("calendar_status", 20)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object MentoringScheduleTable : Table("mentoring_schedule") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id)
    val externalSourceId = varchar("external_source_id", 255)
    val title = varchar("schedule_title", 200)
    val startsAt = datetime("starts_at")
    val endsAt = datetime("ends_at")
    val location = varchar("location", 255).nullable()
    val description = text("description").nullable()
    val googleEventId = varchar("google_event_id", 255).nullable()
    val createdAt = datetime("created_at")

    init {
        uniqueIndex("ux_mentoring_schedule_team_id_external_source_id", teamId, externalSourceId)
        index("ix_mentoring_schedule_team_id_starts_at_ends_at", false, teamId, startsAt, endsAt)
    }

    override val primaryKey = PrimaryKey(id)
}

object When2meetLinkTable : Table("when2meet_link") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id).uniqueIndex()
    val url = varchar("url", 2048)
    val status = enumerationByName<When2meetLinkStatus>("link_status", 20)
    val failureReason = varchar("failure_reason", 500).nullable()
    val lastParsedAt = datetime("last_parsed_at").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object AvailabilitySlotTable : Table("availability_slot") {
    val id = long("id").autoIncrement()
    val teamId = long("team_id").references(TeamTable.id)
    val teamCalendarId = long("team_calendar_id").references(TeamCalendarTable.id).nullable()
    val when2meetLinkId = long("when2meet_link_id").references(When2meetLinkTable.id).nullable()
    val availabilitySource = enumerationByName<AvailabilitySource>("availability_source", 30)
    val startsAt = datetime("starts_at")
    val endsAt = datetime("ends_at")
    val availableMemberCount = integer("available_member_count")
    val busyMemberCount = integer("busy_member_count")
    val createdAt = datetime("created_at")

    init {
        index("ix_availability_slot_team_id_starts_at_ends_at", false, teamId, startsAt, endsAt)
    }

    override val primaryKey = PrimaryKey(id)
}

internal fun ResultRow.toTeamCalendarEntity(): TeamCalendarEntity = TeamCalendarEntity(
    id = this[TeamCalendarTable.id],
    teamId = this[TeamCalendarTable.teamId],
    googleCalendarId = this[TeamCalendarTable.googleCalendarId],
    activatedByUserId = this[TeamCalendarTable.activatedByUserId],
    status = this[TeamCalendarTable.status],
    createdAt = this[TeamCalendarTable.createdAt],
    updatedAt = this[TeamCalendarTable.updatedAt],
)

internal fun ResultRow.toMentoringScheduleEntity(): MentoringScheduleEntity = MentoringScheduleEntity(
    id = this[MentoringScheduleTable.id],
    teamId = this[MentoringScheduleTable.teamId],
    externalSourceId = this[MentoringScheduleTable.externalSourceId],
    title = this[MentoringScheduleTable.title],
    startsAt = this[MentoringScheduleTable.startsAt],
    endsAt = this[MentoringScheduleTable.endsAt],
    location = this[MentoringScheduleTable.location],
    description = this[MentoringScheduleTable.description],
    googleEventId = this[MentoringScheduleTable.googleEventId],
    createdAt = this[MentoringScheduleTable.createdAt],
)

internal fun ResultRow.toWhen2meetLinkEntity(): When2meetLinkEntity = When2meetLinkEntity(
    id = this[When2meetLinkTable.id],
    teamId = this[When2meetLinkTable.teamId],
    url = this[When2meetLinkTable.url],
    status = this[When2meetLinkTable.status],
    failureReason = this[When2meetLinkTable.failureReason],
    lastParsedAt = this[When2meetLinkTable.lastParsedAt],
    createdAt = this[When2meetLinkTable.createdAt],
    updatedAt = this[When2meetLinkTable.updatedAt],
)

internal fun ResultRow.toAvailabilitySlotEntity(): AvailabilitySlotEntity = AvailabilitySlotEntity(
    id = this[AvailabilitySlotTable.id],
    teamId = this[AvailabilitySlotTable.teamId],
    source = this[AvailabilitySlotTable.availabilitySource],
    startsAt = this[AvailabilitySlotTable.startsAt],
    endsAt = this[AvailabilitySlotTable.endsAt],
    availableMemberCount = this[AvailabilitySlotTable.availableMemberCount],
    busyMemberCount = this[AvailabilitySlotTable.busyMemberCount],
    createdAt = this[AvailabilitySlotTable.createdAt],
)
