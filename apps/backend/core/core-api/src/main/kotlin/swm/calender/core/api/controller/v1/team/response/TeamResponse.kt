package swm.calender.core.api.controller.v1.team.response

import swm.calender.core.api.controller.v1.team.service.SubServiceActivationSnapshot
import swm.calender.core.api.controller.v1.team.service.TeamMemberSnapshot
import swm.calender.core.api.controller.v1.team.service.TeamSnapshot
import swm.calender.core.enums.TeamMemberRole

data class TeamResponse(
    val teamId: Long,
    val name: String,
    val description: String?,
    val inviteCode: String,
    val calendarEnabled: Boolean,
    val matchEnabled: Boolean,
) {
    companion object {
        fun from(snapshot: TeamSnapshot): TeamResponse {
            return TeamResponse(
                teamId = snapshot.teamId,
                name = snapshot.name,
                description = snapshot.description,
                inviteCode = snapshot.inviteCode,
                calendarEnabled = snapshot.calendarEnabled,
                matchEnabled = snapshot.matchEnabled,
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
        fun from(snapshot: TeamMemberSnapshot): TeamMemberResponse {
            return TeamMemberResponse(
                memberId = snapshot.memberId,
                userId = snapshot.userId,
                name = snapshot.name,
                email = snapshot.email,
                role = snapshot.role,
            )
        }
    }
}

data class TeamMembersResponse(
    val items: List<TeamMemberResponse>,
)

data class SubServiceActivationResponse(
    val teamId: Long,
    val calendarEnabled: Boolean,
    val matchEnabled: Boolean,
) {
    companion object {
        fun from(snapshot: SubServiceActivationSnapshot): SubServiceActivationResponse {
            return SubServiceActivationResponse(
                teamId = snapshot.teamId,
                calendarEnabled = snapshot.calendarEnabled,
                matchEnabled = snapshot.matchEnabled,
            )
        }
    }
}
