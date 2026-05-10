package swm.calender.match.service.response

import swm.calender.core.common.id.TeamId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CandidateIdeaVisibility
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.DuplicateAnalysisStatus
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SimilarityLevel
import swm.calender.core.enums.SourceDisclosure
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.domain.model.DuplicateAnalysis
import swm.calender.match.domain.model.DuplicateAnalysisMatch
import java.time.Instant

data class CandidateIdeaResponse(
    val candidateIdeaId: Long,
    val teamId: TeamId,
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
        fun from(candidateIdea: CandidateIdea): CandidateIdeaResponse {
            return CandidateIdeaResponse(
                candidateIdeaId = requireNotNull(candidateIdea.id).value,
                teamId = candidateIdea.teamId,
                title = candidateIdea.title,
                summary = candidateIdea.summary,
                problem = candidateIdea.problem,
                targetUsers = candidateIdea.targetUsers,
                solution = candidateIdea.solution,
                category = candidateIdea.category,
                platforms = candidateIdea.platforms,
                visibility = candidateIdea.visibility,
                createdAt = candidateIdea.createdAt,
                updatedAt = candidateIdea.updatedAt,
            )
        }
    }
}

data class DuplicateAnalysisResponse(
    val analysisId: Long,
    val candidateIdeaId: Long,
    val status: DuplicateAnalysisStatus,
    val scannedReleasedServiceCount: Int,
    val scannedCandidateIdeaCount: Int,
    val matches: List<DuplicateAnalysisMatchResponse>,
    val failureReason: String?,
    val generatedAt: Instant,
) {
    companion object {
        fun from(duplicateAnalysis: DuplicateAnalysis): DuplicateAnalysisResponse {
            return DuplicateAnalysisResponse(
                analysisId = requireNotNull(duplicateAnalysis.id).value,
                candidateIdeaId = duplicateAnalysis.candidateIdeaId.value,
                status = duplicateAnalysis.status,
                scannedReleasedServiceCount = duplicateAnalysis.scannedReleasedServiceCount,
                scannedCandidateIdeaCount = duplicateAnalysis.scannedCandidateIdeaCount,
                matches = duplicateAnalysis.matches.map(DuplicateAnalysisMatchResponse::from),
                failureReason = duplicateAnalysis.failureReason,
                generatedAt = duplicateAnalysis.generatedAt,
            )
        }
    }
}

data class DuplicateAnalysisMatchResponse(
    val sourceType: DuplicateAnalysisSourceType,
    val sourceDisclosure: SourceDisclosure,
    val sourceId: Long?,
    val sourceTeamId: TeamId?,
    val sourceTitle: String?,
    val similarityLevel: SimilarityLevel,
    val overlapDimensions: List<OverlapDimension>,
    val overlapSummary: String,
) {
    companion object {
        fun from(match: DuplicateAnalysisMatch): DuplicateAnalysisMatchResponse {
            return DuplicateAnalysisMatchResponse(
                sourceType = match.sourceType,
                sourceDisclosure = match.sourceDisclosure,
                sourceId = match.sourceId,
                sourceTeamId = match.sourceTeamId,
                sourceTitle = match.sourceTitle,
                similarityLevel = match.similarityLevel,
                overlapDimensions = match.overlapDimensions,
                overlapSummary = match.overlapSummary,
            )
        }
    }
}
