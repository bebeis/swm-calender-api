package swm.calender.core.team.service.response

import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.domain.model.TeamMember

data class TeamResponse(
    val teamId: Long,
    val name: String,
    val description: String?,
    val inviteCode: String,
    val calendarEnabled: Boolean,
    val matchEnabled: Boolean,
) {
    companion object {
        fun from(team: Team): TeamResponse {
            return TeamResponse(
                teamId = team.requireId().value,
                name = team.name,
                description = team.description,
                inviteCode = team.inviteCode,
                calendarEnabled = team.subServiceActivation.calendarEnabled,
                matchEnabled = team.subServiceActivation.matchEnabled,
            )
        }
    }
}

data class TeamMemberResponse(
    val memberId: Long,
    val userId: Long,
    val name: String,
    val email: String,
    val role: TeamMemberRole,
) {
    companion object {
        fun from(member: TeamMember): TeamMemberResponse {
            return TeamMemberResponse(
                memberId = requireNotNull(member.id).value,
                userId = member.userId.value,
                name = member.name,
                email = member.email,
                role = member.role,
            )
        }
    }
}
