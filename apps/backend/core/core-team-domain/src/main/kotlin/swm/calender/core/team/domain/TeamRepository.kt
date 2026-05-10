package swm.calender.core.team.domain

import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.domain.model.TeamMemberHistory

interface TeamRepository {
    fun save(team: Team): Team

    fun saveMemberHistory(history: TeamMemberHistory): TeamMemberHistory

    fun findById(teamId: TeamId): Team?

    fun findMemberHistoriesByTeamId(teamId: TeamId): List<TeamMemberHistory>

    fun findByInviteCode(inviteCode: String): Team?

    fun findActiveByUserId(userId: UserId): Team?

    fun existsActiveMembershipByUserId(userId: UserId): Boolean
}
