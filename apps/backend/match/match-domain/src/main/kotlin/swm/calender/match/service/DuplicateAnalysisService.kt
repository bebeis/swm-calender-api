package swm.calender.match.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swm.calender.core.common.id.UserId
import swm.calender.core.team.domain.model.Team
import swm.calender.core.team.exception.TeamDomainException
import swm.calender.core.team.exception.TeamErrorMessage
import swm.calender.core.team.implement.TeamReader
import swm.calender.match.domain.model.DuplicateAnalysis
import swm.calender.match.exception.DuplicateIdeaAnalyzerException
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.CandidateIdeaReader
import swm.calender.match.implement.DuplicateAnalysisWriter
import swm.calender.match.implement.DuplicateCandidateIdeaInput
import swm.calender.match.implement.DuplicateIdeaAnalysisRequest
import swm.calender.match.implement.DuplicateIdeaAnalyzer
import swm.calender.match.implement.DuplicateReleasedServiceInput
import swm.calender.match.implement.MatchCampaignReader
import swm.calender.match.service.request.DuplicateAnalysisRunRequest
import swm.calender.match.service.response.DuplicateAnalysisResponse
import java.time.Clock
import java.time.Instant

@Service
class DuplicateAnalysisService(
    private val teamReader: TeamReader,
    private val candidateIdeaReader: CandidateIdeaReader,
    private val duplicateAnalysisWriter: DuplicateAnalysisWriter,
    private val matchCampaignReader: MatchCampaignReader,
    private val duplicateIdeaAnalyzer: DuplicateIdeaAnalyzer,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun runDuplicateAnalysis(request: DuplicateAnalysisRunRequest): DuplicateAnalysisResponse {
        val team = getMatchEnabledTeam(request.actorUserId)
        requireOwner(team, request.actorUserId)
        val teamId = team.requireId()
        val candidateIdea = candidateIdeaReader.getById(request.candidateIdeaId)
            .requireOwnedBy(teamId)
        val releasedServices = matchCampaignReader.getReleasedServiceProfiles()
        val candidateIdeas = candidateIdeaReader.getAll()
        val analysisRequest = DuplicateIdeaAnalysisRequest(
            candidateIdea = candidateIdea,
            releasedServices = releasedServices.map {
                DuplicateReleasedServiceInput(
                    serviceProfile = it.serviceProfile,
                    openCampaignDescriptions = it.openCampaignDescriptions,
                )
            },
            privateCandidateIdeas = candidateIdeas.map(::DuplicateCandidateIdeaInput),
        )
        val analysis = try {
            DuplicateAnalysis.completed(
                candidateIdeaId = request.candidateIdeaId,
                requestedByTeamId = teamId,
                requestedByUserId = request.actorUserId,
                scannedReleasedServiceCount = releasedServices.size,
                scannedCandidateIdeaCount = candidateIdeas.size,
                matches = duplicateIdeaAnalyzer.analyze(analysisRequest),
                generatedAt = now(),
            )
        } catch (exception: DuplicateIdeaAnalyzerException) {
            DuplicateAnalysis.failed(
                candidateIdeaId = request.candidateIdeaId,
                requestedByTeamId = teamId,
                requestedByUserId = request.actorUserId,
                scannedReleasedServiceCount = releasedServices.size,
                scannedCandidateIdeaCount = candidateIdeas.size,
                failureReason = duplicateAnalysisFailureReason(exception),
                generatedAt = now(),
            )
        }

        return DuplicateAnalysisResponse.from(
            duplicateAnalysisWriter.save(analysis),
        )
    }

    private fun getMatchEnabledTeam(actorUserId: UserId): Team {
        val team = teamReader.getActiveByUserId(actorUserId)
        team.requireMember(actorUserId)

        if (!team.subServiceActivation.matchEnabled) {
            throw MatchDomainException(MatchErrorMessage.MATCH_SUB_SERVICE_DISABLED)
        }

        return team
    }

    private fun requireOwner(
        team: Team,
        actorUserId: UserId,
    ) {
        if (!team.isOwner(actorUserId)) {
            throw TeamDomainException(TeamErrorMessage.TEAM_OWNER_REQUIRED)
        }
    }

    private fun now(): Instant = Instant.now(clock)

    private fun duplicateAnalysisFailureReason(exception: DuplicateIdeaAnalyzerException): String {
        return exception.errorMessage.message.take(MAX_FAILURE_REASON_LENGTH)
    }

    private companion object {
        const val MAX_FAILURE_REASON_LENGTH = 300
    }
}
