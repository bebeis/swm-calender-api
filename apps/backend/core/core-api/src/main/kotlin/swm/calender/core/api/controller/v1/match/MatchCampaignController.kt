package swm.calender.core.api.controller.v1.match

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import swm.calender.core.api.controller.v1.match.request.CampaignCreateRequest
import swm.calender.core.api.controller.v1.match.request.CampaignStatusChangeRequest
import swm.calender.core.api.controller.v1.match.request.ServiceProfileCreateRequest
import swm.calender.core.api.controller.v1.match.response.CampaignResponse
import swm.calender.core.api.controller.v1.match.response.CampaignSearchResponse
import swm.calender.core.api.controller.v1.match.response.ServiceProfileResponse
import swm.calender.core.api.controller.v1.match.service.MatchCampaignApiFacade
import swm.calender.core.api.controller.v1.match.service.MatchRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.support.response.ApiResponse

@RestController
class MatchCampaignController(
    private val matchCampaignApiFacade: MatchCampaignApiFacade,
    private val matchRequestValidator: MatchRequestValidator,
) {
    @PostMapping("/api/v1/match/service-profiles")
    fun createServiceProfile(
        authentication: Authentication?,
        @RequestBody request: ServiceProfileCreateRequest,
    ): ResponseEntity<ApiResponse<ServiceProfileResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.validateServiceProfileCreate(request)
            ServiceProfileResponse.from(
                matchCampaignApiFacade.createServiceProfile(user, request),
            )
        }
    }

    @GetMapping("/api/v1/match/campaigns")
    fun searchCampaigns(
        authentication: Authentication?,
        @RequestParam category: String?,
        @RequestParam platform: String?,
        @RequestParam reciprocalAvailable: String?,
        @RequestParam sort: String?,
    ): ResponseEntity<ApiResponse<CampaignSearchResponse>> {
        AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            CampaignSearchResponse.from(
                matchCampaignApiFacade.searchCampaigns(
                    category = matchRequestValidator.parseCategory(category),
                    platform = matchRequestValidator.parsePlatform(platform),
                    reciprocalAvailable = matchRequestValidator.parseReciprocalAvailable(reciprocalAvailable),
                    sort = matchRequestValidator.parseSort(sort),
                ),
            )
        }
    }

    @PostMapping("/api/v1/match/campaigns")
    fun createCampaign(
        authentication: Authentication?,
        @RequestBody request: CampaignCreateRequest,
    ): ResponseEntity<ApiResponse<CampaignResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.validateCampaignCreate(request)
            CampaignResponse.from(
                matchCampaignApiFacade.createCampaign(user, request),
            )
        }
    }

    @PatchMapping("/api/v1/match/campaigns/{campaignId}/status")
    fun changeCampaignStatus(
        authentication: Authentication?,
        @PathVariable campaignId: Long,
        @RequestBody request: CampaignStatusChangeRequest,
    ): ResponseEntity<ApiResponse<CampaignResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return matchUnauthorized()
        return handleMatchAction {
            matchRequestValidator.parseId(campaignId, "campaignId")
            matchRequestValidator.validateCampaignStatusChange(request)
            CampaignResponse.from(
                matchCampaignApiFacade.changeCampaignStatus(
                    user = user,
                    campaignId = campaignId,
                    request = request,
                ),
            )
        }
    }
}
