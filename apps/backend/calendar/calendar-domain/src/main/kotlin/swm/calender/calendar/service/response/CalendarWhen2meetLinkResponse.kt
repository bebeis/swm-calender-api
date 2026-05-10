package swm.calender.calendar.service.response

import swm.calender.calendar.domain.model.When2meetLink
import swm.calender.core.enums.When2meetLinkStatus

data class CalendarWhen2meetLinkResponse(
    val url: String,
    val status: When2meetLinkStatus,
    val failureReason: String?,
) {
    companion object {
        fun from(when2meetLink: When2meetLink): CalendarWhen2meetLinkResponse {
            return CalendarWhen2meetLinkResponse(
                url = when2meetLink.url,
                status = when2meetLink.status,
                failureReason = when2meetLink.failureReason,
            )
        }
    }
}
