package swm.calender.core.api.controller.v1.calendar.request

import jakarta.validation.constraints.NotBlank

data class When2meetLinkRequest(
    @field:NotBlank
    val url: String?,
)
