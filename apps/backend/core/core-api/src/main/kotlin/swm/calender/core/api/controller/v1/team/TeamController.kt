package swm.calender.core.api.controller.v1.team

import org.springframework.http.HttpStatus
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
import swm.calender.core.api.controller.v1.team.request.TeamMemberRoleChangeRequest
import swm.calender.core.api.controller.v1.team.response.SubServiceActivationResponse
import swm.calender.core.api.controller.v1.team.response.TeamMemberResponse
import swm.calender.core.api.controller.v1.team.response.TeamMembersResponse
import swm.calender.core.api.controller.v1.team.response.TeamResponse
import swm.calender.core.api.controller.v1.team.service.TeamApiException
import swm.calender.core.api.controller.v1.team.service.TeamApiFacade
import swm.calender.core.api.controller.v1.team.service.TeamRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.enums.SubService
import swm.calender.core.support.error.ErrorType
import swm.calender.core.support.response.ApiResponse
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage

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
        val user = AuthenticatedUser.from(authentication) ?: return unauthorized()
        return handle {
            teamRequestValidator.validateTeamCreate(request)
            TeamResponse.from(teamApiFacade.createTeam(user, request))
        }
    }

    @PostMapping("/api/v1/teams/join")
    fun joinTeam(
        authentication: Authentication?,
        @RequestBody request: TeamJoinRequest,
    ): ResponseEntity<ApiResponse<TeamResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return unauthorized()
        return handle {
            teamRequestValidator.validateTeamJoin(request)
            TeamResponse.from(teamApiFacade.joinTeam(user, request))
        }
    }

    @GetMapping("/api/v1/teams/{teamId}/members")
    fun listMembers(
        authentication: Authentication?,
        @PathVariable teamId: Long,
    ): ResponseEntity<ApiResponse<TeamMembersResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return unauthorized()
        return handle {
            TeamMembersResponse(
                items = teamApiFacade.listMembers(user, teamId.toTeamId()).map(TeamMemberResponse::from),
            )
        }
    }

    @PatchMapping("/api/v1/teams/{teamId}/members/{memberId}/role")
    fun changeMemberRole(
        authentication: Authentication?,
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @RequestBody request: TeamMemberRoleChangeRequest,
    ): ResponseEntity<ApiResponse<TeamMemberResponse>> {
        val user = AuthenticatedUser.from(authentication) ?: return unauthorized()
        return handle {
            teamRequestValidator.validateRoleChange(request)
            TeamMemberResponse.from(
                teamApiFacade.changeMemberRole(
                    user = user,
                    teamId = teamId.toTeamId(),
                    memberId = memberId.toTeamMemberId(),
                    role = requireNotNull(request.role),
                ),
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
        val user = AuthenticatedUser.from(authentication) ?: return unauthorized()
        return handle {
            teamRequestValidator.validateSubServiceActivation(request)
            SubServiceActivationResponse.from(
                teamApiFacade.updateSubServiceActivation(
                    user = user,
                    teamId = teamId.toTeamId(),
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

    private fun Long.toTeamId(): TeamId {
        return try {
            TeamId(this)
        } catch (_: IllegalArgumentException) {
            throw TeamApiException.badRequest("teamId must be positive.")
        }
    }

    private fun Long.toTeamMemberId(): TeamMemberId {
        return try {
            TeamMemberId(this)
        } catch (_: IllegalArgumentException) {
            throw TeamApiException.badRequest("memberId must be positive.")
        }
    }

    private fun <T> handle(action: () -> T): ResponseEntity<ApiResponse<T>> {
        return try {
            ResponseEntity.ok(ApiResponse.success(action()))
        } catch (e: TeamApiException) {
            ResponseEntity
                .status(e.errorType.status)
                .body(ApiResponse.error(e.errorType, mapOf("reason" to e.message)))
        } catch (e: TeamDomainException) {
            val errorType = e.errorMessage.toErrorType()
            ResponseEntity
                .status(errorType.status)
                .body(ApiResponse.error(errorType, mapOf("reason" to e.message)))
        }
    }

    private fun <T> unauthorized(): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ErrorType.AUTHENTICATION_REQUIRED))
    }

    private fun TeamErrorMessage.toErrorType(): ErrorType {
        return when (this) {
            TeamErrorMessage.TEAM_NAME_REQUIRED,
            TeamErrorMessage.INVITE_CODE_REQUIRED,
            TeamErrorMessage.TEAM_ACTIVE_OWNER_REQUIRED,
            TeamErrorMessage.TEAM_MEMBER_NAME_REQUIRED,
            TeamErrorMessage.TEAM_MEMBER_EMAIL_REQUIRED,
            TeamErrorMessage.TEAM_MEMBER_INACTIVE,
            TeamErrorMessage.TEAM_NOT_PERSISTED,
            -> ErrorType.VALIDATION_ERROR

            TeamErrorMessage.TEAM_MEMBER_ALREADY_EXISTS,
            TeamErrorMessage.TEAM_ALREADY_EXISTS_FOR_USER,
            -> ErrorType.DUPLICATE_RESOURCE

            TeamErrorMessage.TEAM_OWNER_REQUIRED,
            TeamErrorMessage.TEAM_MEMBER_REQUIRED,
            -> ErrorType.FORBIDDEN

            TeamErrorMessage.TEAM_NOT_FOUND,
            TeamErrorMessage.INVALID_INVITE_CODE,
            TeamErrorMessage.TEAM_MEMBER_NOT_FOUND,
            -> ErrorType.RESOURCE_NOT_FOUND
        }
    }
}
