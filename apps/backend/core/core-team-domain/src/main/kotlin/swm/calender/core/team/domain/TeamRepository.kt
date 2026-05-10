package swm.calender.core.team.domain

import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.model.Team

interface TeamRepository {
    fun save(team: Team): Team

    fun findById(teamId: TeamId): Team?

    fun findByInviteCode(inviteCode: String): Team?

    fun findActiveByUserId(userId: UserId): Team?

    fun existsActiveMembershipByUserId(userId: UserId): Boolean
}
