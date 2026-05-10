package swm.calender.calendar.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import swm.calender.calendar.domain.model.AvailabilitySlot
import swm.calender.calendar.domain.model.MentoringSchedule
import swm.calender.calendar.domain.model.TeamCalendar
import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.common.time.DateTimeRange
import swm.calender.core.enums.AvailabilitySource
import swm.calender.core.enums.CalendarStatus
import swm.calender.core.enums.When2meetLinkStatus
import java.time.Instant
import java.time.OffsetDateTime

class CalendarDomainTest :
    FunSpec({
        test("active team calendar requires a google calendar id") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")

            // when
            val exception = shouldThrow<CalendarDomainException> {
                TeamCalendar(
                    teamId = TeamId(1L),
                    activatedByUserId = UserId(1L),
                    status = CalendarStatus.ACTIVE,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )
            }

            // then
            exception.errorMessage shouldBe CalendarErrorMessage.TEAM_CALENDAR_ID_REQUIRED
        }

        test("mentoring schedule creation trims optional fields and converts to a busy slot") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")
            val range = DateTimeRange(
                startsAt = OffsetDateTime.parse("2026-05-11T10:00:00+09:00"),
                endsAt = OffsetDateTime.parse("2026-05-11T11:00:00+09:00"),
            )

            // when
            val schedule = MentoringSchedule.create(
                teamId = TeamId(1L),
                externalSourceId = " mentor-1 ",
                title = " Weekly mentoring ",
                range = range,
                location = " Room A ",
                description = " Review roadmap ",
                googleEventId = " google-1 ",
                createdAt = createdAt,
            )

            // then
            schedule.externalSourceId shouldBe "mentor-1"
            schedule.title shouldBe "Weekly mentoring"
            schedule.location shouldBe "Room A"
            schedule.description shouldBe "Review roadmap"
            schedule.googleEventId shouldBe "google-1"
            schedule.toBusyAvailabilitySlot() shouldBe AvailabilitySlot(
                teamId = TeamId(1L),
                source = AvailabilitySource.GOOGLE_CALENDAR,
                range = range,
                availableMemberCount = 0,
                busyMemberCount = 1,
                createdAt = createdAt,
            )
        }

        test("replacing a when2meet link resets parsing state to pending") {
            // given
            val createdAt = Instant.parse("2026-05-10T00:00:00Z")
            val existingLink = When2meetLink(
                id = 10L,
                teamId = TeamId(1L),
                url = "https://when2meet.com/?123",
                status = When2meetLinkStatus.FAILED,
                failureReason = "Parser changed",
                lastParsedAt = Instant.parse("2026-05-10T01:00:00Z"),
                createdAt = createdAt,
                updatedAt = Instant.parse("2026-05-10T01:00:00Z"),
            )

            // when
            val replacedLink = existingLink.replace(
                url = "https://when2meet.com/?456",
                replacedAt = Instant.parse("2026-05-10T02:00:00Z"),
            )

            // then
            replacedLink.url shouldBe "https://when2meet.com/?456"
            replacedLink.status shouldBe When2meetLinkStatus.PENDING
            replacedLink.failureReason shouldBe null
            replacedLink.lastParsedAt shouldBe null
        }

        test("availability slot counts must be zero or positive") {
            // given
            val range = DateTimeRange(
                startsAt = OffsetDateTime.parse("2026-05-11T10:00:00+09:00"),
                endsAt = OffsetDateTime.parse("2026-05-11T11:00:00+09:00"),
            )

            // when
            val exception = shouldThrow<CalendarDomainException> {
                AvailabilitySlot(
                    teamId = TeamId(1L),
                    source = AvailabilitySource.WHEN2MEET,
                    range = range,
                    availableMemberCount = -1,
                    busyMemberCount = 0,
                    createdAt = Instant.parse("2026-05-10T00:00:00Z"),
                )
            }

            // then
            exception.errorMessage shouldBe CalendarErrorMessage.AVAILABILITY_COUNT_NEGATIVE
        }
    })
