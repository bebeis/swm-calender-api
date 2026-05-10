package swm.calender.core.api.controller.v1.team

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import swm.calender.core.api.controller.v1.team.request.SubServiceActivationRequest
import swm.calender.core.api.controller.v1.team.request.TeamCreateRequest
import swm.calender.core.api.controller.v1.team.request.TeamJoinRequest
import swm.calender.core.api.controller.v1.team.response.SubServiceActivationResponse
import swm.calender.core.api.controller.v1.team.response.TeamMemberResponse
import swm.calender.core.api.controller.v1.team.response.TeamMembersResponse
import swm.calender.core.api.controller.v1.team.response.TeamResponse
import swm.calender.core.api.controller.v1.team.service.TeamApiException
import swm.calender.core.api.controller.v1.team.service.TeamApiFacade
import swm.calender.core.api.controller.v1.team.service.TeamRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.enums.SubService
import swm.calender.core.support.response.ApiResponse

@RestController
class TeamController(
    private val teamApiFacade: TeamApiFacade,
    private val teamRequestValidator: TeamRequestValidator,
) {
    @PostMapping("/api/v1/teams")
    fun createTeam(
        authentication: Authentication?,
        @RequestBody request: TeamCreateRequest,
    ): ResponseEntity<ApiResponse<TeamResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return teamUnauthorized()
        return handleTeamAction {
            teamRequestValidator.validateTeamCreate(request)
            TeamResponse.from(teamApiFacade.createTeam(user, request))
        }
    }

    @PostMapping("/api/v1/teams/join")
    fun joinTeam(
        authentication: Authentication?,
        @RequestBody request: TeamJoinRequest,
    ): ResponseEntity<ApiResponse<TeamResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return teamUnauthorized()
        return handleTeamAction {
            teamRequestValidator.validateTeamJoin(request)
            TeamResponse.from(teamApiFacade.joinTeam(user, request))
        }
    }

    @GetMapping("/api/v1/teams/{teamId}/members")
    fun listMembers(
        authentication: Authentication?,
        @PathVariable teamId: Long,
    ): ResponseEntity<ApiResponse<TeamMembersResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return teamUnauthorized()
        return handleTeamAction {
            TeamMembersResponse(
                items = teamApiFacade.listMembers(user, teamId.toTeamIdParameter()).map(TeamMemberResponse::from),
            )
        }
    }

    @PatchMapping("/api/v1/teams/{teamId}/sub-services/{subService}")
    fun updateSubServiceActivation(
        authentication: Authentication?,
        @PathVariable teamId: Long,
        @PathVariable subService: String,
        @RequestBody request: SubServiceActivationRequest,
    ): ResponseEntity<ApiResponse<SubServiceActivationResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return teamUnauthorized()
        return handleTeamAction {
            teamRequestValidator.validateSubServiceActivation(request)
            SubServiceActivationResponse.from(
                teamApiFacade.updateSubServiceActivation(
                    user = user,
                    teamId = teamId.toTeamIdParameter(),
                    subService = subService.toSubService(),
                    enabled = requireNotNull(request.enabled),
                ),
            )
        }
    }

    private fun String.toSubService(): SubService {
        return SubService.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: throw TeamApiException.badRequest("subService must be one of calendar or match.")
    }
}
