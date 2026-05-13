package swm.calender.match.implement

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SimilarityLevel
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.domain.model.DuplicateAnalysisMatch
import swm.calender.match.domain.model.ServiceProfile

interface DuplicateIdeaAnalyzer {
    fun analyze(request: DuplicateIdeaAnalysisRequest): List<DuplicateAnalysisMatch>
}

data class DuplicateIdeaAnalysisRequest(
    val candidateIdea: CandidateIdea,
    val releasedServices: List<DuplicateReleasedServiceInput>,
    val privateCandidateIdeas: List<DuplicateCandidateIdeaInput>,
)

data class DuplicateReleasedServiceInput(
    val serviceProfile: ServiceProfile,
    val openCampaignDescriptions: List<String>,
)

data class DuplicateCandidateIdeaInput(
    val candidateIdea: CandidateIdea,
)

@Component
@ConditionalOnProperty(
    prefix = "swm.match.duplicate-analysis",
    name = ["provider"],
    havingValue = "keyword",
    matchIfMissing = true,
)
class KeywordDuplicateIdeaAnalyzer : DuplicateIdeaAnalyzer {
    override fun analyze(request: DuplicateIdeaAnalysisRequest): List<DuplicateAnalysisMatch> {
        val releasedMatches = request.releasedServices.mapNotNull {
            analyzeReleasedService(request.candidateIdea, it)
        }
        val candidateMatches = request.privateCandidateIdeas
            .filterNot { it.candidateIdea.id == request.candidateIdea.id }
            .mapNotNull { analyzeCandidateIdea(request.candidateIdea, it.candidateIdea) }

        return (releasedMatches + candidateMatches)
            .sortedWith(compareByDescending<DuplicateAnalysisMatch> { it.similarityLevel.rank() }.thenBy { it.sourceTitle })
    }

    private fun analyzeReleasedService(
        target: CandidateIdea,
        input: DuplicateReleasedServiceInput,
    ): DuplicateAnalysisMatch? {
        val dimensions = overlapDimensions(
            target = target,
            sourceText = listOf(
                input.serviceProfile.summary,
                input.serviceProfile.description,
            ) + input.openCampaignDescriptions,
            sourcePlatforms = input.serviceProfile.platforms,
        )
        if (dimensions.isEmpty()) {
            return null
        }

        return DuplicateAnalysisMatch.releasedService(
            sourceId = requireNotNull(input.serviceProfile.id),
            sourceTeamId = input.serviceProfile.teamId,
            sourceTitle = input.serviceProfile.name,
            similarityLevel = dimensions.toSimilarityLevel(),
            overlapDimensions = dimensions,
            overlapSummary = "Released service overlaps on ${dimensions.joinToString(", ") { it.name.lowercase() }}.",
        )
    }

    private fun analyzeCandidateIdea(
        target: CandidateIdea,
        source: CandidateIdea,
    ): DuplicateAnalysisMatch? {
        val dimensions = overlapDimensions(
            target = target,
            sourceText = listOf(source.summary, source.problem, source.targetUsers, source.solution),
            sourcePlatforms = source.platforms,
        )
        if (dimensions.isEmpty()) {
            return null
        }

        return if (source.teamId == target.teamId) {
            DuplicateAnalysisMatch.ownCandidateIdea(
                sourceId = requireNotNull(source.id).value,
                sourceTeamId = source.teamId,
                sourceTitle = source.title,
                similarityLevel = dimensions.toSimilarityLevel(),
                overlapDimensions = dimensions,
                overlapSummary = "Own team candidate idea overlaps on ${dimensions.joinToString(", ") { it.name.lowercase() }}.",
            )
        } else {
            DuplicateAnalysisMatch.redactedPrivateCandidateIdea(
                similarityLevel = dimensions.toSimilarityLevel(),
                overlapDimensions = dimensions,
                overlapSummary = "Another team's private candidate idea has non-identifying overlap.",
            )
        }
    }

    private fun overlapDimensions(
        target: CandidateIdea,
        sourceText: List<String>,
        sourcePlatforms: List<Platform>,
    ): List<OverlapDimension> {
        val sourceBlob = sourceText.joinToString(" ").lowercase()
        return buildList {
            if (hasSharedKeyword(target.problem, sourceBlob)) add(OverlapDimension.PROBLEM)
            if (hasSharedKeyword(target.targetUsers, sourceBlob)) add(OverlapDimension.TARGET_USER)
            if (hasSharedKeyword(target.solution, sourceBlob)) add(OverlapDimension.SOLUTION)
            if (target.platforms.any(sourcePlatforms::contains)) add(OverlapDimension.PLATFORM)
        }.distinct()
    }

    private fun hasSharedKeyword(
        value: String,
        sourceBlob: String,
    ): Boolean {
        return value
            .lowercase()
            .split(Regex("[^a-z0-9가-힣]+"))
            .filter { it.length >= MIN_KEYWORD_LENGTH }
            .any(sourceBlob::contains)
    }

    private fun List<OverlapDimension>.toSimilarityLevel(): SimilarityLevel {
        return when {
            size >= HIGH_SIMILARITY_DIMENSION_COUNT -> SimilarityLevel.HIGH
            size >= MEDIUM_SIMILARITY_DIMENSION_COUNT -> SimilarityLevel.MEDIUM
            else -> SimilarityLevel.LOW
        }
    }

    private fun SimilarityLevel.rank(): Int {
        return when (this) {
            SimilarityLevel.LOW -> 1
            SimilarityLevel.MEDIUM -> 2
            SimilarityLevel.HIGH -> 3
        }
    }

    private companion object {
        const val MIN_KEYWORD_LENGTH = 2
        const val MEDIUM_SIMILARITY_DIMENSION_COUNT = 2
        const val HIGH_SIMILARITY_DIMENSION_COUNT = 3
    }
}
