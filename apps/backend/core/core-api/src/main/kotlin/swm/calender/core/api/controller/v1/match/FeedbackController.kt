package swm.calender.core.api.controller.v1.match

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import swm.calender.core.api.controller.v1.match.request.FeedbackSubmitRequest
import swm.calender.core.api.controller.v1.match.response.FeedbackResponse
import swm.calender.core.api.controller.v1.match.response.TeamTestHistoryResponse
import swm.calender.core.api.controller.v1.match.service.FeedbackApiFacade
import swm.calender.core.api.controller.v1.match.service.MatchRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.support.response.ApiResponse

@RestController
class FeedbackController(
    private val feedbackApiFacade: FeedbackApiFacade,
    private val matchRequestValidator: MatchRequestValidator,
) {
    @PostMapping("/api/v1/match/assignments/{assignmentId}/feedback")
    fun submitFeedback(
        authentication: Authentication?,
        @PathVariable assignmentId: Long,
        @RequestBody request: FeedbackSubmitRequest,
    ): ResponseEntity<ApiResponse<FeedbackResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.parseId(assignmentId, "assignmentId")
            matchRequestValidator.validateFeedbackSubmit(request)
            FeedbackResponse.from(
                feedbackApiFacade.submitFeedback(
                    user = user,
                    assignmentId = assignmentId,
                    request = request,
                ),
            )
        }
    }

    @GetMapping("/api/v1/match/test-history")
    fun getTeamTestHistory(
        authentication: Authentication?,
    ): ResponseEntity<ApiResponse<TeamTestHistoryResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            TeamTestHistoryResponse.from(feedbackApiFacade.getTeamTestHistory(user))
        }
    }
}
