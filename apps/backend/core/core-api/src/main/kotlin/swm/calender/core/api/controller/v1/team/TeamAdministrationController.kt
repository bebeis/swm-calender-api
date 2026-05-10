package swm.calender.core.api.controller.v1.team

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import swm.calender.core.api.controller.v1.team.request.TeamMemberRoleChangeRequest
import swm.calender.core.api.controller.v1.team.response.TeamMemberResponse
import swm.calender.core.api.controller.v1.team.service.TeamAdministrationApiFacade
import swm.calender.core.api.controller.v1.team.service.TeamRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.support.response.ApiResponse

@RestController
class TeamAdministrationController(
    private val teamAdministrationApiFacade: TeamAdministrationApiFacade,
    private val teamRequestValidator: TeamRequestValidator,
) {
    @PatchMapping("/api/v1/teams/{teamId}/members/{memberId}/role")
    fun changeMemberRole(
        authentication: Authentication?,
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @RequestBody request: TeamMemberRoleChangeRequest,
    ): ResponseEntity<ApiResponse<TeamMemberResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return teamUnauthorized()
        return handleTeamAction {
            teamRequestValidator.validateRoleChange(request)
            TeamMemberResponse.from(
                teamAdministrationApiFacade.changeMemberRole(
                    user = user,
                    teamId = teamId.toTeamIdParameter(),
                    memberId = memberId.toTeamMemberIdParameter(),
                    request = request,
                ),
            )
        }
    }

    @DeleteMapping("/api/v1/teams/{teamId}/members/{memberId}")
    fun removeMember(
        authentication: Authentication?,
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
    ): ResponseEntity<ApiResponse<TeamMemberResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return teamUnauthorized()
        return handleTeamAction {
            TeamMemberResponse.from(
                teamAdministrationApiFacade.removeMember(
                    user = user,
                    teamId = teamId.toTeamIdParameter(),
                    memberId = memberId.toTeamMemberIdParameter(),
                ),
            )
        }
    }
}
