package swm.calender.match.implement

import org.springframework.stereotype.Component
import swm.calender.match.domain.CandidateIdeaRepository
import swm.calender.match.domain.model.CandidateIdea

@Component
class CandidateIdeaWriter(
    private val candidateIdeaRepository: CandidateIdeaRepository,
) {
    fun save(candidateIdea: CandidateIdea): CandidateIdea {
        return candidateIdeaRepository.save(candidateIdea)
    }
}
