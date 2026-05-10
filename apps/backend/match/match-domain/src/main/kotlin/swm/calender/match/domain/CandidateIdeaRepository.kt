package swm.calender.match.domain

import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.TeamId
import swm.calender.match.domain.model.CandidateIdea

interface CandidateIdeaRepository {
    fun save(candidateIdea: CandidateIdea): CandidateIdea

    fun findById(candidateIdeaId: CandidateIdeaId): CandidateIdea?

    fun findByTeamId(teamId: TeamId): List<CandidateIdea>

    fun findAll(): List<CandidateIdea>
}
