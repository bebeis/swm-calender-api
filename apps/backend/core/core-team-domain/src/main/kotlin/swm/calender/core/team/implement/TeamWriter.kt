package swm.calender.core.team.implement

import org.springframework.stereotype.Component
import swm.calender.core.team.domain.TeamRepository
import swm.calender.core.team.domain.model.Team

@Component
class TeamWriter(
    private val teamRepository: TeamRepository,
) {
    fun save(team: Team): Team {
        return teamRepository.save(team)
    }
}
