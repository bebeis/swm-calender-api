package swm.calender.match.domain.model

import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.DuplicateAnalysisId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.DuplicateAnalysisStatus
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.SimilarityLevel
import swm.calender.core.enums.SourceDisclosure
import swm.calender.match.exception.MatchDomainException
import swm.calender.match.exception.MatchErrorMessage
import java.time.Instant

data class DuplicateAnalysis(
    val id: DuplicateAnalysisId? = null,
    val candidateIdeaId: CandidateIdeaId,
    val requestedByTeamId: TeamId,
    val requestedByUserId: UserId,
    val status: DuplicateAnalysisStatus,
    val scannedReleasedServiceCount: Int,
    val scannedCandidateIdeaCount: Int,
    val matches: List<DuplicateAnalysisMatch>,
    val failureReason: String? = null,
    val generatedAt: Instant,
) {
    companion object {
        fun completed(
            candidateIdeaId: CandidateIdeaId,
            requestedByTeamId: TeamId,
            requestedByUserId: UserId,
            scannedReleasedServiceCount: Int,
            scannedCandidateIdeaCount: Int,
            matches: List<DuplicateAnalysisMatch>,
            generatedAt: Instant,
        ): DuplicateAnalysis {
            return DuplicateAnalysis(
                candidateIdeaId = candidateIdeaId,
                requestedByTeamId = requestedByTeamId,
                requestedByUserId = requestedByUserId,
                status = DuplicateAnalysisStatus.COMPLETED,
                scannedReleasedServiceCount = scannedReleasedServiceCount,
                scannedCandidateIdeaCount = scannedCandidateIdeaCount,
                matches = matches,
                generatedAt = generatedAt,
            )
        }

        fun failed(
            candidateIdeaId: CandidateIdeaId,
            requestedByTeamId: TeamId,
            requestedByUserId: UserId,
            scannedReleasedServiceCount: Int,
            scannedCandidateIdeaCount: Int,
            failureReason: String,
            generatedAt: Instant,
        ): DuplicateAnalysis {
            return DuplicateAnalysis(
                candidateIdeaId = candidateIdeaId,
                requestedByTeamId = requestedByTeamId,
                requestedByUserId = requestedByUserId,
                status = DuplicateAnalysisStatus.FAILED,
                scannedReleasedServiceCount = scannedReleasedServiceCount,
                scannedCandidateIdeaCount = scannedCandidateIdeaCount,
                matches = emptyList(),
                failureReason = failureReason.trim().takeIf { it.isNotEmpty() },
                generatedAt = generatedAt,
            )
        }
    }
}

data class DuplicateAnalysisMatch(
    val sourceType: DuplicateAnalysisSourceType,
    val sourceId: Long?,
    val sourceTeamId: TeamId?,
    val sourceTitle: String?,
    val sourceDisclosure: SourceDisclosure,
    val similarityLevel: SimilarityLevel,
    val overlapDimensions: List<OverlapDimension>,
    val overlapSummary: String,
) {
    init {
        if (overlapDimensions.isEmpty()) {
            throw MatchDomainException(MatchErrorMessage.DUPLICATE_ANALYSIS_MATCH_DIMENSIONS_REQUIRED)
        }
        if (
            sourceType == DuplicateAnalysisSourceType.PRIVATE_CANDIDATE_IDEA &&
            sourceDisclosure == SourceDisclosure.REDACTED &&
            (sourceId != null || sourceTeamId != null || !sourceTitle.isNullOrBlank())
        ) {
            throw MatchDomainException(MatchErrorMessage.DUPLICATE_ANALYSIS_PRIVATE_SOURCE_REDACTION_REQUIRED)
        }
    }

    companion object {
        fun releasedService(
            sourceId: Long,
            sourceTeamId: TeamId,
            sourceTitle: String,
            similarityLevel: SimilarityLevel,
            overlapDimensions: List<OverlapDimension>,
            overlapSummary: String,
        ): DuplicateAnalysisMatch {
            return DuplicateAnalysisMatch(
                sourceType = DuplicateAnalysisSourceType.RELEASED_SERVICE,
                sourceId = sourceId,
                sourceTeamId = sourceTeamId,
                sourceTitle = sourceTitle,
                sourceDisclosure = SourceDisclosure.PUBLIC,
                similarityLevel = similarityLevel,
                overlapDimensions = overlapDimensions.distinct(),
                overlapSummary = overlapSummary.trim(),
            )
        }

        fun ownCandidateIdea(
            sourceId: Long,
            sourceTeamId: TeamId,
            sourceTitle: String,
            similarityLevel: SimilarityLevel,
            overlapDimensions: List<OverlapDimension>,
            overlapSummary: String,
        ): DuplicateAnalysisMatch {
            return DuplicateAnalysisMatch(
                sourceType = DuplicateAnalysisSourceType.PRIVATE_CANDIDATE_IDEA,
                sourceId = sourceId,
                sourceTeamId = sourceTeamId,
                sourceTitle = sourceTitle,
                sourceDisclosure = SourceDisclosure.OWN_TEAM,
                similarityLevel = similarityLevel,
                overlapDimensions = overlapDimensions.distinct(),
                overlapSummary = overlapSummary.trim(),
            )
        }

        fun redactedPrivateCandidateIdea(
            similarityLevel: SimilarityLevel,
            overlapDimensions: List<OverlapDimension>,
            overlapSummary: String,
        ): DuplicateAnalysisMatch {
            return DuplicateAnalysisMatch(
                sourceType = DuplicateAnalysisSourceType.PRIVATE_CANDIDATE_IDEA,
                sourceId = null,
                sourceTeamId = null,
                sourceTitle = null,
                sourceDisclosure = SourceDisclosure.REDACTED,
                similarityLevel = similarityLevel,
                overlapDimensions = overlapDimensions.distinct(),
                overlapSummary = overlapSummary.trim(),
            )
        }
    }
}
