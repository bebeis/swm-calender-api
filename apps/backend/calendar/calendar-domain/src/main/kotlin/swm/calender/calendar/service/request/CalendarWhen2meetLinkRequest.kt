package swm.calender.calendar.service.request

import swm.calender.core.common.id.UserId

data class CalendarWhen2meetLinkRequest(
    val actorUserId: UserId,
    val url: String,
)
