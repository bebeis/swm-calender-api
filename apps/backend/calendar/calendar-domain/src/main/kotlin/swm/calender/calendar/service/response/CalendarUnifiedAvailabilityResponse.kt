package swm.calender.calendar.service.response

import swm.calender.calendar.domain.model.UnifiedAvailability
import swm.calender.calendar.domain.model.UnifiedAvailabilitySlot
import java.time.Instant
import java.time.OffsetDateTime

data class CalendarUnifiedAvailabilityResponse(
    val slots: List<CalendarAvailabilitySlotResponse>,
    val generatedAt: Instant,
) {
    companion object {
        fun from(unifiedAvailability: UnifiedAvailability): CalendarUnifiedAvailabilityResponse {
            return CalendarUnifiedAvailabilityResponse(
                slots = unifiedAvailability.slots.map(CalendarAvailabilitySlotResponse::from),
                generatedAt = unifiedAvailability.generatedAt,
            )
        }
    }
}

data class CalendarAvailabilitySlotResponse(
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val availableMemberCount: Int,
    val busyMemberCount: Int,
) {
    companion object {
        fun from(slot: UnifiedAvailabilitySlot): CalendarAvailabilitySlotResponse {
            return CalendarAvailabilitySlotResponse(
                startsAt = slot.range.startsAt,
                endsAt = slot.range.endsAt,
                availableMemberCount = slot.availableMemberCount,
                busyMemberCount = slot.busyMemberCount,
            )
        }
    }
}
