package swm.calender.calendar.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.calendar.domain.model.AvailabilitySlot
import swm.calender.calendar.domain.model.MentoringSchedule
import swm.calender.calendar.domain.model.UnifiedAvailability
import swm.calender.calendar.domain.model.UnifiedAvailabilitySlot
import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.calendar.exception.CalendarDomainException
import swm.calender.calendar.exception.CalendarErrorMessage
import swm.calender.calendar.implement.CalendarReader
import swm.calender.calendar.implement.CalendarWriter
import swm.calender.calendar.service.request.CalendarMentoringScheduleBulkPushRequest
import swm.calender.calendar.service.request.CalendarUnifiedAvailabilityRequest
import swm.calender.calendar.service.request.CalendarWhen2meetLinkRequest
import swm.calender.calendar.service.response.CalendarMentoringScheduleBulkPushResponse
import swm.calender.calendar.service.response.CalendarUnifiedAvailabilityResponse
import swm.calender.calendar.service.response.CalendarWhen2meetLinkResponse
import swm.calender.client.google.calendar.GoogleCalendarClient
import swm.calender.client.google.calendar.GoogleCalendarCreateEventRequest
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.common.time.DateTimeRange
import swm.calender.core.enums.AvailabilitySource
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.core.team.implement.TeamReader
import java.time.Clock
import java.time.Instant

@Service
class CalendarService(
    private val teamReader: TeamReader,
    private val calendarReader: CalendarReader,
    private val calendarWriter: CalendarWriter,
    private val googleCalendarClient: GoogleCalendarClient,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun bulkPushMentoringSchedules(
        request: CalendarMentoringScheduleBulkPushRequest,
    ): CalendarMentoringScheduleBulkPushResponse {
        val team = getCalendarEnabledTeam(request.actorUserId)
        val teamId = team.requireId()
        team.requireMember(request.actorUserId)

        val teamCalendar = calendarReader.getActiveTeamCalendar(teamId)
        val distinctSchedules = request.schedules.distinctBy { it.externalSourceId.trim() }
        val existingSchedules = calendarReader.getMentoringSchedulesByExternalSourceIds(
            teamId = teamId,
            externalSourceIds = distinctSchedules.map { it.externalSourceId.trim() },
        )
        val existingSourceIds = existingSchedules.map { it.externalSourceId }.toSet()
        val createdAt = now()
        val schedulesToSave = distinctSchedules
            .filterNot { it.externalSourceId.trim() in existingSourceIds }
            .map { schedule ->
                val googleEvent = googleCalendarClient.createEvent(
                    GoogleCalendarCreateEventRequest(
                        googleCalendarId = requireNotNull(teamCalendar.googleCalendarId),
                        externalSourceId = schedule.externalSourceId.trim(),
                        title = schedule.title.trim(),
                        range = schedule.toRange(),
                        location = schedule.location,
                        description = schedule.description,
                    ),
                )

                MentoringSchedule.create(
                    teamId = teamId,
                    externalSourceId = schedule.externalSourceId,
                    title = schedule.title,
                    range = schedule.toRange(),
                    location = schedule.location,
                    description = schedule.description,
                    googleEventId = googleEvent.googleEventId,
                    createdAt = createdAt,
                )
            }

        val savedSchedules = calendarWriter.saveMentoringSchedules(schedulesToSave)

        return CalendarMentoringScheduleBulkPushResponse(
            createdCount = savedSchedules.size,
            skippedDuplicateCount = request.schedules.size - savedSchedules.size,
        )
    }

    @Transactional
    fun registerWhen2meetLink(
        request: CalendarWhen2meetLinkRequest,
    ): CalendarWhen2meetLinkResponse {
        val team = getCalendarEnabledTeam(request.actorUserId)
        val teamId = team.requireId()
        requireOwner(team, request.actorUserId)

        val replacedAt = now()
        val when2meetLink = calendarReader.getWhen2meetLink(teamId)
            ?.replace(request.url, replacedAt)
            ?: When2meetLink.create(
                teamId = teamId,
                url = request.url,
                createdAt = replacedAt,
            )

        return CalendarWhen2meetLinkResponse.from(
            calendarWriter.saveWhen2meetLink(when2meetLink),
        )
    }

    @Transactional(readOnly = true)
    fun getUnifiedAvailability(
        request: CalendarUnifiedAvailabilityRequest,
    ): CalendarUnifiedAvailabilityResponse {
        val team = getCalendarEnabledTeam(request.actorUserId)
        val teamId = team.requireId()
        team.requireMember(request.actorUserId)

        val queryRange = request.toRange()
        val googleAvailabilitySlots = calendarReader.getMentoringSchedules(
            teamId = teamId,
            range = queryRange,
        ).map(MentoringSchedule::toBusyAvailabilitySlot)
        val when2meetSlots = calendarReader.getAvailabilitySlots(
            teamId = teamId,
            range = queryRange,
        ).filter { it.source == AvailabilitySource.WHEN2MEET }

        val unifiedAvailability = mergeAvailability(
            teamId = teamId,
            queryRange = queryRange,
            slots = googleAvailabilitySlots + when2meetSlots,
            generatedAt = now(),
        )

        return CalendarUnifiedAvailabilityResponse.from(unifiedAvailability)
    }

    private fun getCalendarEnabledTeam(actorUserId: UserId): Team {
        val team = teamReader.getActiveByUserId(actorUserId)
        team.requireMember(actorUserId)

        if (!team.subServiceActivation.calendarEnabled) {
            throw CalendarDomainException(CalendarErrorMessage.CALENDAR_SUB_SERVICE_DISABLED)
        }

        return team
    }

    private fun requireOwner(
        team: Team,
        actorUserId: UserId,
    ) {
        if (!team.isOwner(actorUserId)) {
            throw TeamDomainException(TeamErrorMessage.TEAM_OWNER_REQUIRED)
        }
    }

    private fun mergeAvailability(
        teamId: TeamId,
        queryRange: DateTimeRange,
        slots: List<AvailabilitySlot>,
        generatedAt: Instant,
    ): UnifiedAvailability {
        val clippedSlots = slots.mapNotNull { clipToQueryRange(it, queryRange) }
        if (clippedSlots.isEmpty()) {
            return UnifiedAvailability(
                teamId = teamId,
                range = queryRange,
                slots = emptyList(),
                generatedAt = generatedAt,
            )
        }

        val boundaries = (
            clippedSlots.flatMap { listOf(it.range.startsAt, it.range.endsAt) } +
                listOf(queryRange.startsAt, queryRange.endsAt)
            ).distinct()
            .sorted()

        val segmentedSlots = boundaries.zipWithNext()
            .mapNotNull { (startsAt, endsAt) ->
                if (!endsAt.isAfter(startsAt)) {
                    return@mapNotNull null
                }

                val segmentRange = DateTimeRange(
                    startsAt = startsAt,
                    endsAt = endsAt,
                )
                val overlappingSlots = clippedSlots.filter { it.overlaps(segmentRange) }
                if (overlappingSlots.isEmpty()) {
                    return@mapNotNull null
                }

                UnifiedAvailabilitySlot(
                    range = segmentRange,
                    availableMemberCount = overlappingSlots.sumOf { it.availableMemberCount },
                    busyMemberCount = overlappingSlots.sumOf { it.busyMemberCount },
                )
            }

        return UnifiedAvailability(
            teamId = teamId,
            range = queryRange,
            slots = mergeAdjacentSegments(segmentedSlots),
            generatedAt = generatedAt,
        )
    }

    private fun clipToQueryRange(
        slot: AvailabilitySlot,
        queryRange: DateTimeRange,
    ): AvailabilitySlot? {
        if (!slot.overlaps(queryRange)) {
            return null
        }

        return slot.copy(
            range = DateTimeRange(
                startsAt = maxOf(slot.range.startsAt, queryRange.startsAt),
                endsAt = minOf(slot.range.endsAt, queryRange.endsAt),
            ),
        )
    }

    private fun mergeAdjacentSegments(
        slots: List<UnifiedAvailabilitySlot>,
    ): List<UnifiedAvailabilitySlot> {
        return slots.fold(mutableListOf()) { acc, slot ->
            val previous = acc.lastOrNull()
            if (
                previous != null &&
                previous.availableMemberCount == slot.availableMemberCount &&
                previous.busyMemberCount == slot.busyMemberCount &&
                previous.range.endsAt == slot.range.startsAt
            ) {
                acc[acc.lastIndex] = previous.copy(
                    range = DateTimeRange(
                        startsAt = previous.range.startsAt,
                        endsAt = slot.range.endsAt,
                    ),
                )
            } else {
                acc += slot
            }

            acc
        }
    }

    private fun now(): Instant = Instant.now(clock)
}
