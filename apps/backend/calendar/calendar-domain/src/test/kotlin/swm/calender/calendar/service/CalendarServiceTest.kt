package swm.calender.calendar.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import swm.calender.calendar.domain.model.AvailabilitySlot
import swm.calender.calendar.domain.model.MentoringSchedule
import swm.calender.calendar.domain.model.TeamCalendar
import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.calendar.implement.CalendarReader
import swm.calender.calendar.implement.CalendarWriter
import swm.calender.calendar.service.request.CalendarMentoringScheduleBulkPushRequest
import swm.calender.calendar.service.request.CalendarMentoringScheduleRequestItem
import swm.calender.calendar.service.request.CalendarUnifiedAvailabilityRequest
import swm.calender.calendar.service.request.CalendarWhen2meetLinkRequest
import swm.calender.client.google.calendar.GoogleCalendarClient
import swm.calender.client.google.calendar.GoogleCalendarCreateEventRequest
import swm.calender.client.google.calendar.GoogleCalendarCreateEventResponse
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.common.time.DateTimeRange
import swm.calender.core.enums.AvailabilitySource
import swm.calender.core.enums.CalendarStatus
import swm.calender.core.enums.When2meetLinkStatus
import swm.calender.core.team.domain.model.SubServiceActivation
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.core.team.implement.TeamReader
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class CalendarServiceTest :
    FunSpec({
        val fixedInstant = Instant.parse("2026-05-10T00:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        lateinit var teamReader: TeamReader
        lateinit var calendarReader: CalendarReader
        lateinit var calendarWriter: CalendarWriter
        lateinit var googleCalendarClient: GoogleCalendarClient
        lateinit var calendarService: CalendarService

        beforeTest {
            teamReader = mockk()
            calendarReader = mockk()
            calendarWriter = mockk()
            googleCalendarClient = mockk()
            calendarService = CalendarService(
                teamReader = teamReader,
                calendarReader = calendarReader,
                calendarWriter = calendarWriter,
                googleCalendarClient = googleCalendarClient,
                clock = fixedClock,
            )
        }

        test("bulkPushMentoringSchedules skips existing and duplicated input schedules") {
            // given
            val teamId = TeamId(1L)
            val actorUserId = UserId(10L)
            val existingSchedule = MentoringSchedule.create(
                teamId = teamId,
                externalSourceId = "mentor-1",
                title = "Existing schedule",
                range = scheduleRange(
                    "2026-05-11T10:00:00+09:00",
                    "2026-05-11T11:00:00+09:00",
                ),
                googleEventId = "evt-existing",
                createdAt = fixedInstant,
            )
            val newEventRequest = slot<GoogleCalendarCreateEventRequest>()
            every { teamReader.getActiveByUserId(actorUserId) } returns calendarEnabledTeam(
                teamId = teamId,
                ownerUserId = actorUserId,
            )
            every { calendarReader.getActiveTeamCalendar(teamId) } returns activeTeamCalendar(teamId, actorUserId)
            every {
                calendarReader.getMentoringSchedulesByExternalSourceIds(
                    teamId = teamId,
                    externalSourceIds = any(),
                )
            } returns listOf(existingSchedule)
            every { googleCalendarClient.createEvent(capture(newEventRequest)) } returns GoogleCalendarCreateEventResponse(
                googleEventId = "evt-new",
            )
            every { calendarWriter.saveMentoringSchedules(any()) } answers { firstArg() }

            // when
            val response = calendarService.bulkPushMentoringSchedules(
                CalendarMentoringScheduleBulkPushRequest(
                    actorUserId = actorUserId,
                    schedules = listOf(
                        mentoringScheduleRequestItem(
                            externalSourceId = "mentor-1",
                            startsAt = "2026-05-11T10:00:00+09:00",
                            endsAt = "2026-05-11T11:00:00+09:00",
                        ),
                        mentoringScheduleRequestItem(
                            externalSourceId = "mentor-2",
                            startsAt = "2026-05-11T12:00:00+09:00",
                            endsAt = "2026-05-11T13:00:00+09:00",
                        ),
                        mentoringScheduleRequestItem(
                            externalSourceId = "mentor-2",
                            startsAt = "2026-05-11T12:00:00+09:00",
                            endsAt = "2026-05-11T13:00:00+09:00",
                        ),
                    ),
                ),
            )

            // then
            response.createdCount shouldBe 1
            response.skippedDuplicateCount shouldBe 2
            newEventRequest.captured.googleCalendarId shouldBe "team-calendar@group.calendar.google.com"
            newEventRequest.captured.externalSourceId shouldBe "mentor-2"
            verify(exactly = 1) { googleCalendarClient.createEvent(any()) }
            verify(exactly = 1) {
                calendarWriter.saveMentoringSchedules(
                    match {
                        it.shouldHaveSize(1)
                        it.single().externalSourceId == "mentor-2" &&
                            it.single().googleEventId == "evt-new"
                    },
                )
            }
        }

        test("registerWhen2meetLink replaces the saved link and resets it to pending") {
            // given
            val teamId = TeamId(1L)
            val ownerUserId = UserId(10L)
            every { teamReader.getActiveByUserId(ownerUserId) } returns calendarEnabledTeam(
                teamId = teamId,
                ownerUserId = ownerUserId,
            )
            every { calendarReader.getWhen2meetLink(teamId) } returns When2meetLink(
                id = 5L,
                teamId = teamId,
                url = "https://when2meet.com/?old",
                status = When2meetLinkStatus.PARSED,
                lastParsedAt = Instant.parse("2026-05-09T00:00:00Z"),
                createdAt = Instant.parse("2026-05-09T00:00:00Z"),
                updatedAt = Instant.parse("2026-05-09T00:00:00Z"),
            )
            every { calendarWriter.saveWhen2meetLink(any()) } answers { firstArg() }

            // when
            val response = calendarService.registerWhen2meetLink(
                CalendarWhen2meetLinkRequest(
                    actorUserId = ownerUserId,
                    url = "https://when2meet.com/?new",
                ),
            )

            // then
            response.url shouldBe "https://when2meet.com/?new"
            response.status shouldBe When2meetLinkStatus.PENDING
            response.failureReason shouldBe null
            verify(exactly = 1) {
                calendarWriter.saveWhen2meetLink(
                    match {
                        it.url == "https://when2meet.com/?new" &&
                            it.status == When2meetLinkStatus.PENDING &&
                            it.lastParsedAt == null
                    },
                )
            }
        }

        test("registerWhen2meetLink rejects a non-owner") {
            // given
            val teamId = TeamId(1L)
            val ownerUserId = UserId(10L)
            val memberUserId = UserId(20L)
            every { teamReader.getActiveByUserId(memberUserId) } returns calendarEnabledTeam(
                teamId = teamId,
                ownerUserId = ownerUserId,
                memberUserId = memberUserId,
            )

            // when
            val exception = shouldThrow<TeamDomainException> {
                calendarService.registerWhen2meetLink(
                    CalendarWhen2meetLinkRequest(
                        actorUserId = memberUserId,
                        url = "https://when2meet.com/?123",
                    ),
                )
            }

            // then
            exception.errorMessage shouldBe TeamErrorMessage.TEAM_OWNER_REQUIRED
            verify(exactly = 0) { calendarWriter.saveWhen2meetLink(any()) }
        }

        test("getUnifiedAvailability merges google and when2meet slots into segmented output") {
            // given
            val teamId = TeamId(1L)
            val actorUserId = UserId(10L)
            val queryStartsAt = "2026-05-11T09:00:00+09:00"
            val queryEndsAt = "2026-05-11T12:00:00+09:00"
            every { teamReader.getActiveByUserId(actorUserId) } returns calendarEnabledTeam(
                teamId = teamId,
                ownerUserId = actorUserId,
            )
            every {
                calendarReader.getMentoringSchedules(
                    teamId = teamId,
                    range = scheduleRange(queryStartsAt, queryEndsAt),
                )
            } returns listOf(
                MentoringSchedule.create(
                    teamId = teamId,
                    externalSourceId = "mentor-1",
                    title = "Mentoring",
                    range = scheduleRange(
                        "2026-05-11T10:00:00+09:00",
                        "2026-05-11T11:00:00+09:00",
                    ),
                    createdAt = fixedInstant,
                ),
            )
            every {
                calendarReader.getAvailabilitySlots(
                    teamId = teamId,
                    range = scheduleRange(queryStartsAt, queryEndsAt),
                )
            } returns listOf(
                AvailabilitySlot(
                    teamId = teamId,
                    source = AvailabilitySource.WHEN2MEET,
                    range = scheduleRange(
                        "2026-05-11T09:30:00+09:00",
                        "2026-05-11T10:30:00+09:00",
                    ),
                    availableMemberCount = 3,
                    busyMemberCount = 0,
                    createdAt = fixedInstant,
                ),
                AvailabilitySlot(
                    teamId = teamId,
                    source = AvailabilitySource.WHEN2MEET,
                    range = scheduleRange(
                        "2026-05-11T11:00:00+09:00",
                        "2026-05-11T11:30:00+09:00",
                    ),
                    availableMemberCount = 2,
                    busyMemberCount = 1,
                    createdAt = fixedInstant,
                ),
            )

            // when
            val response = calendarService.getUnifiedAvailability(
                CalendarUnifiedAvailabilityRequest(
                    actorUserId = actorUserId,
                    startsAt = OffsetDateTime.parse(queryStartsAt),
                    endsAt = OffsetDateTime.parse(queryEndsAt),
                ),
            )

            // then
            response.generatedAt shouldBe fixedInstant
            response.slots shouldHaveSize 4
            response.slots[0].startsAt shouldBe OffsetDateTime.parse("2026-05-11T09:30:00+09:00")
            response.slots[0].endsAt shouldBe OffsetDateTime.parse("2026-05-11T10:00:00+09:00")
            response.slots[0].availableMemberCount shouldBe 3
            response.slots[0].busyMemberCount shouldBe 0
            response.slots[1].startsAt shouldBe OffsetDateTime.parse("2026-05-11T10:00:00+09:00")
            response.slots[1].endsAt shouldBe OffsetDateTime.parse("2026-05-11T10:30:00+09:00")
            response.slots[1].availableMemberCount shouldBe 3
            response.slots[1].busyMemberCount shouldBe 1
            response.slots[2].startsAt shouldBe OffsetDateTime.parse("2026-05-11T10:30:00+09:00")
            response.slots[2].endsAt shouldBe OffsetDateTime.parse("2026-05-11T11:00:00+09:00")
            response.slots[2].availableMemberCount shouldBe 0
            response.slots[2].busyMemberCount shouldBe 1
            response.slots[3].startsAt shouldBe OffsetDateTime.parse("2026-05-11T11:00:00+09:00")
            response.slots[3].endsAt shouldBe OffsetDateTime.parse("2026-05-11T11:30:00+09:00")
            response.slots[3].availableMemberCount shouldBe 2
            response.slots[3].busyMemberCount shouldBe 1
        }
    }) {
    companion object {
        private val baseInstant: Instant = Instant.parse("2026-05-09T23:00:00Z")

        private fun activeTeamCalendar(
            teamId: TeamId,
            actorUserId: UserId,
        ): TeamCalendar {
            return TeamCalendar(
                teamId = teamId,
                googleCalendarId = "team-calendar@group.calendar.google.com",
                activatedByUserId = actorUserId,
                status = CalendarStatus.ACTIVE,
                createdAt = baseInstant,
                updatedAt = baseInstant,
            )
        }

        private fun calendarEnabledTeam(
            teamId: TeamId,
            ownerUserId: UserId,
            memberUserId: UserId? = null,
        ): Team {
            val createdTeam = Team.create(
                name = "Team Rocket",
                description = "Prepare for trouble",
                inviteCode = "INVITE123456",
                ownerUserId = ownerUserId,
                ownerName = "Owner",
                ownerEmail = "owner@swm.app",
                createdAt = baseInstant,
            ).changeSubServiceActivation(
                subService = swm.calender.core.enums.SubService.CALENDAR,
                enabled = true,
                actorUserId = ownerUserId,
                occurredAt = baseInstant.plusSeconds(60),
            )

            val teamWithMember = memberUserId?.let {
                createdTeam.addMember(
                    userId = it,
                    name = "Member",
                    email = "member@swm.app",
                    joinedAt = baseInstant.plusSeconds(120),
                )
            } ?: createdTeam

            return teamWithMember.copy(
                id = teamId,
                members = teamWithMember.members.mapIndexed { index, member ->
                    member.copy(
                        id = swm.calender.core.common.id.TeamMemberId((index + 1).toLong()),
                        teamId = teamId,
                    )
                },
                subServiceActivation = SubServiceActivation(
                    calendarEnabled = true,
                    matchEnabled = false,
                    calendarEnabledAt = baseInstant.plusSeconds(60),
                ),
            )
        }

        private fun mentoringScheduleRequestItem(
            externalSourceId: String,
            startsAt: String,
            endsAt: String,
        ): CalendarMentoringScheduleRequestItem {
            return CalendarMentoringScheduleRequestItem(
                externalSourceId = externalSourceId,
                title = "Mentoring $externalSourceId",
                startsAt = OffsetDateTime.parse(startsAt),
                endsAt = OffsetDateTime.parse(endsAt),
            )
        }

        private fun scheduleRange(
            startsAt: String,
            endsAt: String,
        ): DateTimeRange {
            return DateTimeRange(
                startsAt = OffsetDateTime.parse(startsAt),
                endsAt = OffsetDateTime.parse(endsAt),
            )
        }
    }
}
