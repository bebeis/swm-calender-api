package swm.calender.core.api.controller.v1.calendar.service

import org.springframework.stereotype.Component
import swm.calender.calendar.service.CalendarService
import swm.calender.calendar.service.request.CalendarMentoringScheduleBulkPushRequest
import swm.calender.calendar.service.request.CalendarMentoringScheduleRequestItem
import swm.calender.calendar.service.request.CalendarUnifiedAvailabilityRequest
import swm.calender.calendar.service.request.CalendarWhen2meetLinkRequest
import swm.calender.calendar.service.response.CalendarAvailabilitySlotResponse
import swm.calender.calendar.service.response.CalendarMentoringScheduleBulkPushResponse
import swm.calender.calendar.service.response.CalendarUnifiedAvailabilityResponse
import swm.calender.calendar.service.response.CalendarWhen2meetLinkResponse
import swm.calender.core.api.controller.v1.calendar.request.MentoringScheduleBulkPushRequest
import swm.calender.core.api.controller.v1.calendar.request.When2meetLinkRequest
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.enums.When2meetLinkStatus
import java.time.Instant
import java.time.OffsetDateTime

@Component
class CalendarApiFacade(
    private val calendarService: CalendarService,
) {
    fun bulkPushMentoringSchedules(
        user: AuthenticatedUser,
        request: MentoringScheduleBulkPushRequest,
    ): MentoringScheduleBulkPushSnapshot {
        return MentoringScheduleBulkPushSnapshot.from(
            calendarService.bulkPushMentoringSchedules(
                CalendarMentoringScheduleBulkPushRequest(
                    actorUserId = user.userId,
                    schedules = requireNotNull(request.schedules).map {
                        CalendarMentoringScheduleRequestItem(
                            externalSourceId = requireNotNull(it.externalSourceId),
                            title = requireNotNull(it.title),
                            startsAt = requireNotNull(it.startsAt),
                            endsAt = requireNotNull(it.endsAt),
                            location = it.location,
                            description = it.description,
                        )
                    },
                ),
            ),
        )
    }

    fun registerWhen2meetLink(
        user: AuthenticatedUser,
        request: When2meetLinkRequest,
    ): When2meetLinkSnapshot {
        return When2meetLinkSnapshot.from(
            calendarService.registerWhen2meetLink(
                CalendarWhen2meetLinkRequest(
                    actorUserId = user.userId,
                    url = requireNotNull(request.url),
                ),
            ),
        )
    }

    fun getUnifiedAvailability(
        user: AuthenticatedUser,
        startsAt: OffsetDateTime,
        endsAt: OffsetDateTime,
    ): UnifiedAvailabilitySnapshot {
        return UnifiedAvailabilitySnapshot.from(
            calendarService.getUnifiedAvailability(
                CalendarUnifiedAvailabilityRequest(
                    actorUserId = user.userId,
                    startsAt = startsAt,
                    endsAt = endsAt,
                ),
            ),
        )
    }
}

data class MentoringScheduleBulkPushSnapshot(
    val createdCount: Int,
    val skippedDuplicateCount: Int,
) {
    companion object {
        fun from(response: CalendarMentoringScheduleBulkPushResponse): MentoringScheduleBulkPushSnapshot {
            return MentoringScheduleBulkPushSnapshot(
                createdCount = response.createdCount,
                skippedDuplicateCount = response.skippedDuplicateCount,
            )
        }
    }
}

data class When2meetLinkSnapshot(
    val url: String,
    val status: When2meetLinkStatus,
    val failureReason: String?,
) {
    companion object {
        fun from(response: CalendarWhen2meetLinkResponse): When2meetLinkSnapshot {
            return When2meetLinkSnapshot(
                url = response.url,
                status = response.status,
                failureReason = response.failureReason,
            )
        }
    }
}

data class UnifiedAvailabilitySnapshot(
    val slots: List<AvailabilitySlotSnapshot>,
    val generatedAt: Instant,
) {
    companion object {
        fun from(response: CalendarUnifiedAvailabilityResponse): UnifiedAvailabilitySnapshot {
            return UnifiedAvailabilitySnapshot(
                slots = response.slots.map(AvailabilitySlotSnapshot::from),
                generatedAt = response.generatedAt,
            )
        }
    }
}

data class AvailabilitySlotSnapshot(
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val availableMemberCount: Int,
    val busyMemberCount: Int,
) {
    companion object {
        fun from(response: CalendarAvailabilitySlotResponse): AvailabilitySlotSnapshot {
            return AvailabilitySlotSnapshot(
                startsAt = response.startsAt,
                endsAt = response.endsAt,
                availableMemberCount = response.availableMemberCount,
                busyMemberCount = response.busyMemberCount,
            )
        }
    }
}
