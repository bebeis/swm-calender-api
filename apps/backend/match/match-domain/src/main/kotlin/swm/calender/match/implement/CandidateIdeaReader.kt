package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.TeamId
import swm.calender.match.domain.CandidateIdeaRepository
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage

@Component
class CandidateIdeaReader(
    private val candidateIdeaRepository: CandidateIdeaRepository,
) {
    fun getById(candidateIdeaId: CandidateIdeaId): CandidateIdea {
        return candidateIdeaRepository.findById(candidateIdeaId)
            ?: throw MatchDomainException(MatchErrorMessage.CANDIDATE_IDEA_NOT_FOUND)
    }

    fun getByTeamId(teamId: TeamId): List<CandidateIdea> {
        return candidateIdeaRepository.findByTeamId(teamId)
    }

    fun getAll(): List<CandidateIdea> {
        return candidateIdeaRepository.findAll()
    }
}
