package swm.calender.core.api.controller.v1.match.service

import org.springframework.stereotype.Component
import swm.calender.core.api.controller.v1.match.request.CandidateIdeaCreateRequest
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CandidateIdeaVisibility
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.DuplicateAnalysisStatus
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SimilarityLevel
import swm.calender.core.enums.SourceDisclosure
import swm.calender.match.service.CandidateIdeaService
import swm.calender.match.service.DuplicateAnalysisService
import swm.calender.match.service.request.DuplicateAnalysisRunRequest
import java.time.Instant
import swm.calender.match.service.response.CandidateIdeaResponse as CandidateIdeaServiceResponse
import swm.calender.match.service.response.DuplicateAnalysisMatchResponse as DuplicateAnalysisMatchServiceResponse
import swm.calender.match.service.response.DuplicateAnalysisResponse as DuplicateAnalysisServiceResponse

@Component
class CandidateIdeaApiFacade(
    private val candidateIdeaService: CandidateIdeaService,
    private val duplicateAnalysisService: DuplicateAnalysisService,
) {
    fun createCandidateIdea(
        user: AuthenticatedUser,
        request: CandidateIdeaCreateRequest,
    ): CandidateIdeaSnapshot {
        return CandidateIdeaSnapshot.from(
            candidateIdeaService.createCandidateIdea(
                swm.calender.match.service.request.CandidateIdeaCreateRequest(
                    actorUserId = user.userId,
                    title = requireNotNull(request.title),
                    summary = requireNotNull(request.summary),
                    problem = requireNotNull(request.problem),
                    targetUsers = requireNotNull(request.targetUsers),
                    solution = requireNotNull(request.solution),
                    category = requireNotNull(request.category),
                    platforms = requireNotNull(request.platforms),
                ),
            ),
        )
    }

    fun listCandidateIdeas(user: AuthenticatedUser): List<CandidateIdeaSnapshot> {
        return candidateIdeaService.listCandidateIdeas(user.userId)
            .map(CandidateIdeaSnapshot::from)
    }

    fun runDuplicateAnalysis(
        user: AuthenticatedUser,
        candidateIdeaId: Long,
    ): DuplicateAnalysisSnapshot {
        return DuplicateAnalysisSnapshot.from(
            duplicateAnalysisService.runDuplicateAnalysis(
                DuplicateAnalysisRunRequest(
                    actorUserId = user.userId,
                    candidateIdeaId = CandidateIdeaId(candidateIdeaId),
                ),
            ),
        )
    }
}

data class CandidateIdeaSnapshot(
    val candidateIdeaId: Long,
    val teamId: Long,
    val title: String,
    val summary: String,
    val problem: String,
    val targetUsers: String,
    val solution: String,
    val category: CampaignCategory,
    val platforms: List<Platform>,
    val visibility: CandidateIdeaVisibility,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(response: CandidateIdeaServiceResponse): CandidateIdeaSnapshot {
            return CandidateIdeaSnapshot(
                candidateIdeaId = response.candidateIdeaId,
                teamId = response.teamId.value,
                title = response.title,
                summary = response.summary,
                problem = response.problem,
                targetUsers = response.targetUsers,
                solution = response.solution,
                category = response.category,
                platforms = response.platforms,
                visibility = response.visibility,
                createdAt = response.createdAt,
                updatedAt = response.updatedAt,
            )
        }
    }
}

data class DuplicateAnalysisSnapshot(
    val analysisId: Long,
    val candidateIdeaId: Long,
    val status: DuplicateAnalysisStatus,
    val scannedReleasedServiceCount: Int,
    val scannedCandidateIdeaCount: Int,
    val matches: List<DuplicateAnalysisMatchSnapshot>,
    val failureReason: String?,
    val generatedAt: Instant,
) {
    companion object {
        fun from(response: DuplicateAnalysisServiceResponse): DuplicateAnalysisSnapshot {
            return DuplicateAnalysisSnapshot(
                analysisId = response.analysisId,
                candidateIdeaId = response.candidateIdeaId,
                status = response.status,
                scannedReleasedServiceCount = response.scannedReleasedServiceCount,
                scannedCandidateIdeaCount = response.scannedCandidateIdeaCount,
                matches = response.matches.map(DuplicateAnalysisMatchSnapshot::from),
                failureReason = response.failureReason,
                generatedAt = response.generatedAt,
            )
        }
    }
}

data class DuplicateAnalysisMatchSnapshot(
    val sourceType: DuplicateAnalysisSourceType,
    val sourceDisclosure: SourceDisclosure,
    val sourceId: Long?,
    val sourceTeamId: Long?,
    val sourceTitle: String?,
    val similarityLevel: SimilarityLevel,
    val overlapDimensions: List<OverlapDimension>,
    val overlapSummary: String,
) {
    companion object {
        fun from(response: DuplicateAnalysisMatchServiceResponse): DuplicateAnalysisMatchSnapshot {
            return DuplicateAnalysisMatchSnapshot(
                sourceType = response.sourceType,
                sourceDisclosure = response.sourceDisclosure,
                sourceId = response.sourceId,
                sourceTeamId = response.sourceTeamId?.value,
                sourceTitle = response.sourceTitle,
                similarityLevel = response.similarityLevel,
                overlapDimensions = response.overlapDimensions,
                overlapSummary = response.overlapSummary,
            )
        }
    }
}
