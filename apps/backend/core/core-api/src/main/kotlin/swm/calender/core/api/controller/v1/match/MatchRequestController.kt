package swm.calender.core.api.controller.v1.match

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import swm.calender.core.api.controller.v1.match.request.MatchRequestCreateRequest
import swm.calender.core.api.controller.v1.match.request.MatchRequestStatusChangeRequest
import swm.calender.core.api.controller.v1.match.response.AssignmentResponse
import swm.calender.core.api.controller.v1.match.response.MatchRequestResponse
import swm.calender.core.api.controller.v1.match.response.MatchRequestStatusChangeResponse
import swm.calender.core.api.controller.v1.match.service.MatchRequestApiFacade
import swm.calender.core.api.controller.v1.match.service.MatchRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.support.response.ApiResponse

@RestController
class MatchRequestController(
    private val matchRequestApiFacade: MatchRequestApiFacade,
    private val matchRequestValidator: MatchRequestValidator,
) {
    @PostMapping("/api/v1/match/campaigns/{campaignId}/requests")
    fun createRequest(
        authentication: Authentication?,
        @PathVariable campaignId: Long,
        @RequestBody request: MatchRequestCreateRequest,
    ): ResponseEntity<ApiResponse<MatchRequestResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.parseId(campaignId, "campaignId")
            matchRequestValidator.validateMatchRequestCreate(request)
            MatchRequestResponse.from(
                matchRequestApiFacade.createRequest(
                    user = user,
                    campaignId = campaignId,
                    request = request,
                ),
            )
        }
    }

    @PatchMapping("/api/v1/match/requests/{requestId}/status")
    fun changeRequestStatus(
        authentication: Authentication?,
        @PathVariable requestId: Long,
        @RequestBody request: MatchRequestStatusChangeRequest,
    ): ResponseEntity<ApiResponse<MatchRequestStatusChangeResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.parseId(requestId, "requestId")
            matchRequestValidator.validateMatchRequestStatusChange(request)
            MatchRequestStatusChangeResponse.from(
                matchRequestApiFacade.changeRequestStatus(
                    user = user,
                    requestId = requestId,
                    request = request,
                ),
            )
        }
    }

    @GetMapping("/api/v1/match/assignments/{assignmentId}")
    fun getAssignment(
        authentication: Authentication?,
        @PathVariable assignmentId: Long,
    ): ResponseEntity<ApiResponse<AssignmentResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.parseId(assignmentId, "assignmentId")
            AssignmentResponse.from(
                matchRequestApiFacade.getAssignment(
                    user = user,
                    assignmentId = assignmentId,
                ),
            )
        }
    }
}
