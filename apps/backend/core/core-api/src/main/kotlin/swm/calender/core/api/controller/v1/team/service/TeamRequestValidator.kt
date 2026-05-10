package swm.calender.core.api.controller.v1.team.service

import jakarta.validation.Validator
import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.team.request.SubServiceActivationRequest
import swm.calender.core.api.controller.v1.team.request.TeamCreateRequest
import swm.calender.core.api.controller.v1.team.request.TeamJoinRequest
import swm.calender.core.api.controller.v1.team.request.TeamMemberRoleChangeRequest

@Component
class TeamRequestValidator(
    private val validator: Validator,
) {
    fun validateTeamCreate(request: TeamCreateRequest) {
        validateBean(request)
        if (request.name.isBlank()) {
            throw TeamApiException.badRequest("name must not be blank.")
        }
    }

    fun validateTeamJoin(request: TeamJoinRequest) {
        validateBean(request)
        if (request.inviteCode.isBlank()) {
            throw TeamApiException.badRequest("inviteCode must not be blank.")
        }
    }

    fun validateRoleChange(request: TeamMemberRoleChangeRequest) {
        if (request.role == null) {
            throw TeamApiException.badRequest("role is required.")
        }
    }

    fun validateSubServiceActivation(request: SubServiceActivationRequest) {
        if (request.enabled == null) {
            throw TeamApiException.badRequest("enabled is required.")
        }
    }

    private fun validateBean(request: Any) {
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw TeamApiException.badRequest(violations.first().message)
        }
    }
}
