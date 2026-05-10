package swm.calender.core.api.controller.v1.match

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import swm.calender.core.api.controller.v1.match.request.CandidateIdeaCreateRequest
import swm.calender.core.api.controller.v1.match.response.CandidateIdeaListResponse
import swm.calender.core.api.controller.v1.match.response.CandidateIdeaResponse
import swm.calender.core.api.controller.v1.match.response.DuplicateAnalysisResponse
import swm.calender.core.api.controller.v1.match.service.CandidateIdeaApiFacade
import swm.calender.core.api.controller.v1.match.service.MatchRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.support.response.ApiResponse

@RestController
class CandidateIdeaController(
    private val candidateIdeaApiFacade: CandidateIdeaApiFacade,
    private val matchRequestValidator: MatchRequestValidator,
) {
    @GetMapping("/api/v1/match/candidate-ideas")
    fun listCandidateIdeas(
        authentication: Authentication?,
    ): ResponseEntity<ApiResponse<CandidateIdeaListResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            CandidateIdeaListResponse.from(candidateIdeaApiFacade.listCandidateIdeas(user))
        }
    }

    @PostMapping("/api/v1/match/candidate-ideas")
    fun createCandidateIdea(
        authentication: Authentication?,
        @RequestBody request: CandidateIdeaCreateRequest,
    ): ResponseEntity<ApiResponse<CandidateIdeaResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.validateCandidateIdeaCreate(request)
            CandidateIdeaResponse.from(
                candidateIdeaApiFacade.createCandidateIdea(user, request),
            )
        }
    }

    @PostMapping("/api/v1/match/candidate-ideas/{candidateIdeaId}/duplicate-analysis")
    fun runDuplicateAnalysis(
        authentication: Authentication?,
        @PathVariable candidateIdeaId: Long,
    ): ResponseEntity<ApiResponse<DuplicateAnalysisResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.parseId(candidateIdeaId, "candidateIdeaId")
            DuplicateAnalysisResponse.from(
                candidateIdeaApiFacade.runDuplicateAnalysis(
                    user = user,
                    candidateIdeaId = candidateIdeaId,
                ),
            )
        }
    }
}
