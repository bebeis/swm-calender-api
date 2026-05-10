package swm.calender.storage.db.core.calendar

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import swm.calender.calendar.domain.model.TeamCalendar
import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.common.time.DateTimeRange
import swm.calender.core.enums.AvailabilitySource
import swm.calender.core.enums.CalendarStatus
import swm.calender.core.enums.When2meetLinkStatus
import swm.calender.storage.db.core.RepositoryTestSupport
import swm.calender.storage.db.core.team.SubServiceActivationTable
import swm.calender.storage.db.core.team.TeamMemberHistoryTable
import swm.calender.storage.db.core.team.TeamMemberTable
import swm.calender.storage.db.core.team.TeamTable
import java.time.Instant
import java.time.OffsetDateTime

class CalendarExposedRepositoryIT : RepositoryTestSupport() {
    @Autowired
    private lateinit var calendarExposedRepository: CalendarExposedRepository

    init {
        extension(SpringExtension())

        val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        beforeTest {
            transaction {
                AvailabilitySlotTable.deleteAll()
                MentoringScheduleTable.deleteAll()
                When2meetLinkTable.deleteAll()
                TeamCalendarTable.deleteAll()
                TeamMemberHistoryTable.deleteAll()
                TeamMemberTable.deleteAll()
                SubServiceActivationTable.deleteAll()
                TeamTable.deleteAll()
            }
        }

        test("saveTeamCalendar stores and updates the team calendar aggregate") {
            // given
            val teamId = createTeam("Calendar Team", "CALENDAR-TEAM")
            val createdCalendar = TeamCalendar.createAuthRequired(
                teamId = teamId,
                actorUserId = UserId(101L),
                createdAt = baseInstant,
            )

            // when
            val savedCalendar = calendarExposedRepository.saveTeamCalendar(createdCalendar)
            val activatedCalendar = calendarExposedRepository.saveTeamCalendar(
                savedCalendar.activate(
                    googleCalendarId = "team-calendar@group.calendar.google.com",
                    actorUserId = UserId(101L),
                    occurredAt = baseInstant.plusSeconds(300),
                ),
            )
            val foundCalendar = calendarExposedRepository.findTeamCalendarByTeamId(teamId)

            // then
            savedCalendar.status shouldBe CalendarStatus.AUTH_REQUIRED
            activatedCalendar.id shouldBe savedCalendar.id
            activatedCalendar.status shouldBe CalendarStatus.ACTIVE
            activatedCalendar.googleCalendarId shouldBe "team-calendar@group.calendar.google.com"
            foundCalendar shouldBe activatedCalendar
        }

        test("findMentoringSchedulesByExternalSourceIds returns only matching schedules for the team") {
            // given
            val teamId = createTeam("Schedule Team", "SCHEDULE-TEAM")
            val otherTeamId = createTeam("Other Team", "OTHER-TEAM")
            calendarExposedRepository.saveMentoringSchedules(
                listOf(
                    mentoringSchedule(
                        teamId = teamId,
                        externalSourceId = "mentor-1",
                        startsAt = "2026-05-11T10:00:00+09:00",
                        endsAt = "2026-05-11T11:00:00+09:00",
                        createdAt = baseInstant,
                    ),
                    mentoringSchedule(
                        teamId = teamId,
                        externalSourceId = "mentor-2",
                        startsAt = "2026-05-11T12:00:00+09:00",
                        endsAt = "2026-05-11T13:00:00+09:00",
                        createdAt = baseInstant.plusSeconds(60),
                    ),
                    mentoringSchedule(
                        teamId = otherTeamId,
                        externalSourceId = "mentor-1",
                        startsAt = "2026-05-11T14:00:00+09:00",
                        endsAt = "2026-05-11T15:00:00+09:00",
                        createdAt = baseInstant.plusSeconds(120),
                    ),
                ),
            )

            // when
            val foundSchedules = calendarExposedRepository.findMentoringSchedulesByExternalSourceIds(
                teamId = teamId,
                externalSourceIds = listOf("mentor-2", " mentor-1 ", "missing"),
            )

            // then
            foundSchedules.shouldHaveSize(2)
            foundSchedules.map { it.externalSourceId } shouldBe listOf("mentor-1", "mentor-2")
            foundSchedules.map { it.teamId }.distinct() shouldBe listOf(teamId)
        }

        test("saveWhen2meetLink replaces the existing team link instead of inserting another row") {
            // given
            val teamId = createTeam("When2meet Team", "WHEN2MEET-TEAM")
            val savedLink = calendarExposedRepository.saveWhen2meetLink(
                When2meetLink.create(
                    teamId = teamId,
                    url = "https://when2meet.com/?old-link",
                    createdAt = baseInstant,
                ).markParsed(baseInstant.plusSeconds(120)),
            )

            // when
            val replacedLink = calendarExposedRepository.saveWhen2meetLink(
                savedLink.replace(
                    url = "https://when2meet.com/?new-link",
                    replacedAt = baseInstant.plusSeconds(300),
                ),
            )
            val foundLink = calendarExposedRepository.findWhen2meetLinkByTeamId(teamId)
            val linkRowCount = transaction {
                When2meetLinkTable
                    .selectAll()
                    .where { When2meetLinkTable.teamId eq teamId.value }
                    .count()
            }

            // then
            replacedLink.id shouldBe savedLink.id
            replacedLink.status shouldBe When2meetLinkStatus.PENDING
            replacedLink.failureReason shouldBe null
            replacedLink.lastParsedAt shouldBe null
            foundLink shouldBe replacedLink
            linkRowCount shouldBe 1L
        }

        test("findAvailabilitySlotsByTeamIdAndRange returns overlapping slots in start-time order") {
            // given
            val teamId = createTeam("Availability Team", "AVAILABILITY-TEAM")
            val otherTeamId = createTeam("Availability Other Team", "AVAILABILITY-OTHER")
            val savedCalendar = calendarExposedRepository.saveTeamCalendar(
                TeamCalendar(
                    teamId = teamId,
                    googleCalendarId = "team-availability@group.calendar.google.com",
                    activatedByUserId = UserId(301L),
                    status = CalendarStatus.ACTIVE,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                ),
            )
            val savedLink = calendarExposedRepository.saveWhen2meetLink(
                When2meetLink.create(
                    teamId = teamId,
                    url = "https://when2meet.com/?availability",
                    createdAt = baseInstant,
                ),
            )
            val otherCalendar = calendarExposedRepository.saveTeamCalendar(
                TeamCalendar(
                    teamId = otherTeamId,
                    googleCalendarId = "other-team@group.calendar.google.com",
                    activatedByUserId = UserId(302L),
                    status = CalendarStatus.ACTIVE,
                    createdAt = baseInstant,
                    updatedAt = baseInstant,
                ),
            )
            transaction {
                insertAvailabilitySlot(
                    teamId = teamId,
                    teamCalendarId = requireNotNull(savedCalendar.id),
                    when2meetLinkId = null,
                    source = AvailabilitySource.GOOGLE_CALENDAR,
                    startsAt = "2026-05-11T09:00:00+09:00",
                    endsAt = "2026-05-11T10:30:00+09:00",
                    availableMemberCount = 0,
                    busyMemberCount = 1,
                    createdAt = baseInstant,
                )
                insertAvailabilitySlot(
                    teamId = teamId,
                    teamCalendarId = null,
                    when2meetLinkId = requireNotNull(savedLink.id),
                    source = AvailabilitySource.WHEN2MEET,
                    startsAt = "2026-05-11T11:30:00+09:00",
                    endsAt = "2026-05-11T12:30:00+09:00",
                    availableMemberCount = 3,
                    busyMemberCount = 0,
                    createdAt = baseInstant.plusSeconds(60),
                )
                insertAvailabilitySlot(
                    teamId = teamId,
                    teamCalendarId = requireNotNull(savedCalendar.id),
                    when2meetLinkId = null,
                    source = AvailabilitySource.GOOGLE_CALENDAR,
                    startsAt = "2026-05-11T07:00:00+09:00",
                    endsAt = "2026-05-11T08:00:00+09:00",
                    availableMemberCount = 0,
                    busyMemberCount = 1,
                    createdAt = baseInstant.plusSeconds(120),
                )
                insertAvailabilitySlot(
                    teamId = otherTeamId,
                    teamCalendarId = requireNotNull(otherCalendar.id),
                    when2meetLinkId = null,
                    source = AvailabilitySource.GOOGLE_CALENDAR,
                    startsAt = "2026-05-11T10:15:00+09:00",
                    endsAt = "2026-05-11T10:45:00+09:00",
                    availableMemberCount = 0,
                    busyMemberCount = 1,
                    createdAt = baseInstant.plusSeconds(180),
                )
            }

            // when
            val foundSlots = calendarExposedRepository.findAvailabilitySlotsByTeamIdAndRange(
                teamId = teamId,
                range = range(
                    startsAt = "2026-05-11T10:00:00+09:00",
                    endsAt = "2026-05-11T12:00:00+09:00",
                ),
            )

            // then
            foundSlots.shouldHaveSize(2)
            foundSlots.map { it.source } shouldBe listOf(
                AvailabilitySource.GOOGLE_CALENDAR,
                AvailabilitySource.WHEN2MEET,
            )
            foundSlots[0].range.startsAt.toInstant() shouldBe Instant.parse("2026-05-11T00:00:00Z")
            foundSlots[0].range.endsAt.toInstant() shouldBe Instant.parse("2026-05-11T01:30:00Z")
            foundSlots[1].range.startsAt.toInstant() shouldBe Instant.parse("2026-05-11T02:30:00Z")
            foundSlots[1].range.endsAt.toInstant() shouldBe Instant.parse("2026-05-11T03:30:00Z")
        }
    }

    private fun createTeam(
        name: String,
        inviteCode: String,
    ): TeamId = transaction {
        val now = Instant.parse("2026-05-10T00:00:00Z")
        val savedTeamId = TeamTable.insert {
            it[TeamTable.name] = name
            it[description] = "$name description"
            it[TeamTable.inviteCode] = inviteCode
            it[createdAt] = now.toLocalDateTime()
            it[updatedAt] = now.toLocalDateTime()
        }[TeamTable.id]

        SubServiceActivationTable.insert {
            it[teamId] = savedTeamId
            it[calendarEnabled] = true
            it[matchEnabled] = false
            it[calendarEnabledAt] = now.toLocalDateTime()
            it[matchEnabledAt] = null
            it[calendarDisabledAt] = null
            it[matchDisabledAt] = null
        }

        TeamId(savedTeamId)
    }

    private fun mentoringSchedule(
        teamId: TeamId,
        externalSourceId: String,
        startsAt: String,
        endsAt: String,
        createdAt: Instant,
    ) = swm.calender.calendar.domain.model.MentoringSchedule.create(
        teamId = teamId,
        externalSourceId = externalSourceId,
        title = "Mentoring $externalSourceId",
        range = range(startsAt, endsAt),
        createdAt = createdAt,
    )

    private fun range(
        startsAt: String,
        endsAt: String,
    ): DateTimeRange {
        return DateTimeRange(
            startsAt = OffsetDateTime.parse(startsAt),
            endsAt = OffsetDateTime.parse(endsAt),
        )
    }

    private fun insertAvailabilitySlot(
        teamId: TeamId,
        teamCalendarId: Long?,
        when2meetLinkId: Long?,
        source: AvailabilitySource,
        startsAt: String,
        endsAt: String,
        availableMemberCount: Int,
        busyMemberCount: Int,
        createdAt: Instant,
    ) {
        AvailabilitySlotTable.insert {
            it[AvailabilitySlotTable.teamId] = teamId.value
            it[AvailabilitySlotTable.teamCalendarId] = teamCalendarId
            it[AvailabilitySlotTable.when2meetLinkId] = when2meetLinkId
            it[AvailabilitySlotTable.availabilitySource] = source
            it[AvailabilitySlotTable.startsAt] = OffsetDateTime.parse(startsAt).toUtcLocalDateTime()
            it[AvailabilitySlotTable.endsAt] = OffsetDateTime.parse(endsAt).toUtcLocalDateTime()
            it[AvailabilitySlotTable.availableMemberCount] = availableMemberCount
            it[AvailabilitySlotTable.busyMemberCount] = busyMemberCount
            it[AvailabilitySlotTable.createdAt] = createdAt.toLocalDateTime()
        }
    }
}
