package swm.calender.core.api.controller.v1.match.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType

data class MatchRequestCreateRequest(
    @field:NotNull
    val type: MatchRequestType?,
    @field:Size(max = 1000)
    val message: String? = null,
)

data class MatchRequestStatusChangeRequest(
    @field:NotNull
    val status: MatchRequestStatus?,
)
