package swm.calender.core.api.controller.v1.match.response

import swm.calender.core.api.controller.v1.match.service.CandidateIdeaSnapshot
import swm.calender.core.api.controller.v1.match.service.DuplicateAnalysisMatchSnapshot
import swm.calender.core.api.controller.v1.match.service.DuplicateAnalysisSnapshot
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CandidateIdeaVisibility
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.DuplicateAnalysisStatus
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SimilarityLevel
import swm.calender.core.enums.SourceDisclosure
import java.time.Instant

data class CandidateIdeaResponse(
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
        fun from(snapshot: CandidateIdeaSnapshot): CandidateIdeaResponse {
            return CandidateIdeaResponse(
                candidateIdeaId = snapshot.candidateIdeaId,
                teamId = snapshot.teamId,
                title = snapshot.title,
                summary = snapshot.summary,
                problem = snapshot.problem,
                targetUsers = snapshot.targetUsers,
                solution = snapshot.solution,
                category = snapshot.category,
                platforms = snapshot.platforms,
                visibility = snapshot.visibility,
                createdAt = snapshot.createdAt,
                updatedAt = snapshot.updatedAt,
            )
        }
    }
}

data class CandidateIdeaListResponse(
    val items: List<CandidateIdeaResponse>,
) {
    companion object {
        fun from(snapshots: List<CandidateIdeaSnapshot>): CandidateIdeaListResponse {
            return CandidateIdeaListResponse(
                items = snapshots.map(CandidateIdeaResponse::from),
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
        fun from(snapshot: DuplicateAnalysisSnapshot): DuplicateAnalysisResponse {
            return DuplicateAnalysisResponse(
                analysisId = snapshot.analysisId,
                candidateIdeaId = snapshot.candidateIdeaId,
                status = snapshot.status,
                scannedReleasedServiceCount = snapshot.scannedReleasedServiceCount,
                scannedCandidateIdeaCount = snapshot.scannedCandidateIdeaCount,
                matches = snapshot.matches.map(DuplicateAnalysisMatchResponse::from),
                failureReason = snapshot.failureReason,
                generatedAt = snapshot.generatedAt,
            )
        }
    }
}

data class DuplicateAnalysisMatchResponse(
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
        fun from(snapshot: DuplicateAnalysisMatchSnapshot): DuplicateAnalysisMatchResponse {
            return DuplicateAnalysisMatchResponse(
                sourceType = snapshot.sourceType,
                sourceDisclosure = snapshot.sourceDisclosure,
                sourceId = snapshot.sourceId,
                sourceTeamId = snapshot.sourceTeamId,
                sourceTitle = snapshot.sourceTitle,
                similarityLevel = snapshot.similarityLevel,
                overlapDimensions = snapshot.overlapDimensions,
                overlapSummary = snapshot.overlapSummary,
            )
        }
    }
}
