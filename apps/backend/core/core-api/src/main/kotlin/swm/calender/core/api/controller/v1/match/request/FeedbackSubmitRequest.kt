package swm.calender.core.api.controller.v1.match.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FeedbackSubmitRequest(
    @field:Valid
    val scores: FeedbackScoresRequest,
    @field:NotBlank
    @field:Size(min = 10, max = 1000)
    val summary: String,
    @field:Size(max = 1000)
    val improvementSuggestion: String? = null,
)

data class FeedbackScoresRequest(
    @field:Min(1)
    @field:Max(5)
    val usability: Int,
    @field:Min(1)
    @field:Max(5)
    val value: Int,
    @field:Min(1)
    @field:Max(5)
    val reliability: Int,
    @field:Min(1)
    @field:Max(5)
    val recommendation: Int,
)
