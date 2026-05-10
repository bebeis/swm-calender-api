package swm.calender.core.team.implement

import org.springframework.stereotype.Component
import swm.calender.core.team.domain.TeamRepository
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.domain.model.TeamMemberHistory

@Component
class TeamWriter(
    private val teamRepository: TeamRepository,
) {
    fun save(team: Team): Team {
        return teamRepository.save(team)
    }

    fun saveMemberHistory(history: TeamMemberHistory): TeamMemberHistory {
        return teamRepository.saveMemberHistory(history)
    }
}
