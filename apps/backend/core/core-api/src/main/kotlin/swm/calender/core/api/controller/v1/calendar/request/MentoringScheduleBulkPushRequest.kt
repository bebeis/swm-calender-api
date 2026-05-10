package swm.calender.core.api.controller.v1.calendar.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

data class MentoringScheduleBulkPushRequest(
    @field:Valid
    @field:NotEmpty
    val schedules: List<MentoringScheduleRequestItem>?,
)

data class MentoringScheduleRequestItem(
    @field:NotBlank
    val externalSourceId: String?,
    @field:NotBlank
    val title: String?,
    @field:NotNull
    val startsAt: OffsetDateTime?,
    @field:NotNull
    val endsAt: OffsetDateTime?,
    val location: String? = null,
    val description: String? = null,
)
