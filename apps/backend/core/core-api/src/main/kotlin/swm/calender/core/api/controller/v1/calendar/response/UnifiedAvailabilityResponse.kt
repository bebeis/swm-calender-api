package swm.calender.core.api.controller.v1.calendar.response

import swm.calender.core.api.controller.v1.calendar.service.AvailabilitySlotSnapshot
import swm.calender.core.api.controller.v1.calendar.service.MentoringScheduleBulkPushSnapshot
import swm.calender.core.api.controller.v1.calendar.service.UnifiedAvailabilitySnapshot
import swm.calender.core.api.controller.v1.calendar.service.When2meetLinkSnapshot
import swm.calender.core.enums.When2meetLinkStatus
import java.time.Instant
import java.time.OffsetDateTime

data class MentoringScheduleBulkPushResponse(
    val createdCount: Int,
    val skippedDuplicateCount: Int,
) {
    companion object {
        fun from(snapshot: MentoringScheduleBulkPushSnapshot): MentoringScheduleBulkPushResponse {
            return MentoringScheduleBulkPushResponse(
                createdCount = snapshot.createdCount,
                skippedDuplicateCount = snapshot.skippedDuplicateCount,
            )
        }
    }
}

data class When2meetLinkResponse(
    val url: String,
    val status: When2meetLinkStatus,
    val failureReason: String?,
) {
    companion object {
        fun from(snapshot: When2meetLinkSnapshot): When2meetLinkResponse {
            return When2meetLinkResponse(
                url = snapshot.url,
                status = snapshot.status,
                failureReason = snapshot.failureReason,
            )
        }
    }
}

data class UnifiedAvailabilityResponse(
    val slots: List<AvailabilitySlotResponse>,
    val generatedAt: Instant,
) {
    companion object {
        fun from(snapshot: UnifiedAvailabilitySnapshot): UnifiedAvailabilityResponse {
            return UnifiedAvailabilityResponse(
                slots = snapshot.slots.map(AvailabilitySlotResponse::from),
                generatedAt = snapshot.generatedAt,
            )
        }
    }
}

data class AvailabilitySlotResponse(
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val availableMemberCount: Int,
    val busyMemberCount: Int,
) {
    companion object {
        fun from(snapshot: AvailabilitySlotSnapshot): AvailabilitySlotResponse {
            return AvailabilitySlotResponse(
                startsAt = snapshot.startsAt,
                endsAt = snapshot.endsAt,
                availableMemberCount = snapshot.availableMemberCount,
                busyMemberCount = snapshot.busyMemberCount,
            )
        }
    }
}
