package swm.calender.match.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.CandidateIdeaReader
import swm.calender.match.implement.CandidateIdeaWriter
import swm.calender.match.service.request.CandidateIdeaCreateRequest
import swm.calender.match.service.response.CandidateIdeaResponse
import java.time.Clock
import java.time.Instant

@Service
class CandidateIdeaService(
    private val teamReader: TeamReader,
    private val candidateIdeaReader: CandidateIdeaReader,
    private val candidateIdeaWriter: CandidateIdeaWriter,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun createCandidateIdea(request: CandidateIdeaCreateRequest): CandidateIdeaResponse {
        val team = getMatchEnabledTeam(request.actorUserId)
        team.requireMember(request.actorUserId)
        val teamId = team.requireId()

        return CandidateIdeaResponse.from(
            candidateIdeaWriter.save(
                CandidateIdea.createPrivate(
                    teamId = teamId,
                    title = request.title,
                    summary = request.summary,
                    problem = request.problem,
                    targetUsers = request.targetUsers,
                    solution = request.solution,
                    category = request.category,
                    platforms = request.platforms,
                    createdByUserId = request.actorUserId,
                    createdAt = now(),
                ),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun listCandidateIdeas(actorUserId: UserId): List<CandidateIdeaResponse> {
        val team = getMatchEnabledTeam(actorUserId)
        team.requireMember(actorUserId)

        return candidateIdeaReader.getByTeamId(team.requireId())
            .map(CandidateIdeaResponse::from)
    }

    private fun getMatchEnabledTeam(actorUserId: UserId): Team {
        val team = teamReader.getActiveByUserId(actorUserId)
        team.requireMember(actorUserId)

        if (!team.subServiceActivation.matchEnabled) {
            throw MatchDomainException(MatchErrorMessage.MATCH_SUB_SERVICE_DISABLED)
        }

        return team
    }

    private fun now(): Instant = Instant.now(clock)
}
