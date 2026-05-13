package swm.calender.client.gemini

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.SimilarityLevel
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.domain.model.DuplicateAnalysisMatch
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.match.exception.DuplicateIdeaAnalyzerException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.DuplicateCandidateIdeaInput
import swm.calender.match.implement.DuplicateIdeaAnalysisRequest
import swm.calender.match.implement.DuplicateIdeaAnalyzer
import swm.calender.match.implement.DuplicateReleasedServiceInput

@Component
@ConditionalOnProperty(
    prefix = "swm.match.duplicate-analysis",
    name = ["provider"],
    havingValue = "gemini",
)
class GeminiDuplicateIdeaAnalyzer(
    private val properties: GeminiDuplicateAnalysisProperties,
    restClientBuilder: RestClient.Builder,
    private val objectMapper: ObjectMapper,
) : DuplicateIdeaAnalyzer {
    private val restClient = restClientBuilder
        .baseUrl(properties.baseUrl.trim().removeSuffix("/"))
        .build()

    override fun analyze(request: DuplicateIdeaAnalysisRequest): List<DuplicateAnalysisMatch> {
        requireApiKey()

        val prompt = buildPrompt(request)
        val responseText = requestGemini(prompt)
        val response = parseResponse(responseText)
        val releasedServices = request.releasedServices
            .mapNotNull { input -> input.serviceProfile.id?.let { it to input.serviceProfile } }
            .toMap()
        val candidateIdeas = request.privateCandidateIdeas
            .mapNotNull { input -> input.candidateIdea.id?.value?.let { it to input.candidateIdea } }
            .toMap()

        return response.matches
            .asSequence()
            .mapNotNull { it.toDomainMatch(request.candidateIdea, releasedServices, candidateIdeas) }
            .distinctBy { listOf(it.sourceType, it.sourceId, it.sourceDisclosure, it.overlapSummary) }
            .sortedWith(compareByDescending<DuplicateAnalysisMatch> { it.similarityLevel.rank() }.thenBy { it.sourceTitle })
            .take(properties.maxMatches)
            .toList()
    }

    private fun requireApiKey() {
        if (properties.apiKey.isBlank()) {
            throw DuplicateIdeaAnalyzerException(MatchErrorMessage.DUPLICATE_ANALYSIS_PROVIDER_API_KEY_REQUIRED)
        }
    }

    private fun requestGemini(prompt: String): String {
        val response = try {
            restClient.post()
                .uri("/v1beta/models/{model}:generateContent", properties.model.removePrefix("models/"))
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", properties.apiKey)
                .body(GeminiGenerateContentRequest.forPrompt(prompt, properties))
                .retrieve()
                .body(GeminiGenerateContentResponse::class.java)
        } catch (exception: RestClientException) {
            throw DuplicateIdeaAnalyzerException(MatchErrorMessage.DUPLICATE_ANALYSIS_PROVIDER_REQUEST_FAILED, exception)
        }

        return response
            ?.candidates
            ?.asSequence()
            ?.flatMap { it.content?.parts.orEmpty().asSequence() }
            ?.mapNotNull { it.text?.trim()?.takeIf(String::isNotEmpty) }
            ?.firstOrNull()
            ?: throw DuplicateIdeaAnalyzerException(MatchErrorMessage.DUPLICATE_ANALYSIS_PROVIDER_EMPTY_RESPONSE)
    }

    private fun parseResponse(responseText: String): GeminiDuplicateAnalysisResponse {
        return try {
            objectMapper.readValue(responseText)
        } catch (exception: RuntimeException) {
            throw DuplicateIdeaAnalyzerException(
                MatchErrorMessage.DUPLICATE_ANALYSIS_PROVIDER_RESPONSE_PARSE_FAILED,
                exception,
            )
        }
    }

    private fun buildPrompt(request: DuplicateIdeaAnalysisRequest): String {
        val target = request.candidateIdea.toPromptCandidate(TeamScope.OWN_TEAM)
        val releasedServices = request.releasedServices
            .asSequence()
            .take(properties.maxReleasedServices)
            .mapNotNull(DuplicateReleasedServiceInput::toPromptReleasedService)
            .toList()
        val privateCandidateIdeas = request.privateCandidateIdeas
            .asSequence()
            .map(DuplicateCandidateIdeaInput::candidateIdea)
            .filterNot { it.id == request.candidateIdea.id }
            .take(properties.maxCandidateIdeas)
            .map { candidateIdea ->
                candidateIdea.toPromptCandidate(
                    if (candidateIdea.teamId == request.candidateIdea.teamId) {
                        TeamScope.OWN_TEAM
                    } else {
                        TeamScope.OTHER_TEAM
                    },
                )
            }
            .toList()

        return """
            You compare Software Maestro team product ideas for overlap.
            Return JSON only. Use only sourceId values from the corpus.
            Report overlaps across problem, target user, solution, feature, platform, and business model.
            For another team's private candidate idea, never reveal its title, team, source text, or identifying details in overlapSummary.
            If there is no meaningful overlap, return {"matches":[]}.

            Target candidate:
            ${objectMapper.writeValueAsString(target)}

            Public released services:
            ${objectMapper.writeValueAsString(releasedServices)}

            Private candidate ideas:
            ${objectMapper.writeValueAsString(privateCandidateIdeas)}
        """.trimIndent()
    }

    private fun GeminiDuplicateAnalysisMatch.toDomainMatch(
        target: CandidateIdea,
        releasedServices: Map<Long, ServiceProfile>,
        candidateIdeas: Map<Long, CandidateIdea>,
    ): DuplicateAnalysisMatch? {
        val sourceType = parseEnum<DuplicateAnalysisSourceType>(sourceType) ?: return null
        val sourceId = sourceId ?: return null
        val similarityLevel = parseEnum<SimilarityLevel>(similarityLevel) ?: return null
        val dimensions = overlapDimensions
            .mapNotNull { parseEnum<OverlapDimension>(it) }
            .distinct()
        if (dimensions.isEmpty()) {
            return null
        }

        return when (sourceType) {
            DuplicateAnalysisSourceType.RELEASED_SERVICE -> {
                val serviceProfile = releasedServices[sourceId] ?: return null
                DuplicateAnalysisMatch.releasedService(
                    sourceId = sourceId,
                    sourceTeamId = serviceProfile.teamId,
                    sourceTitle = serviceProfile.name,
                    similarityLevel = similarityLevel,
                    overlapDimensions = dimensions,
                    overlapSummary = normalizedSummary(overlapSummary),
                )
            }

            DuplicateAnalysisSourceType.PRIVATE_CANDIDATE_IDEA -> {
                val candidateIdea = candidateIdeas[sourceId] ?: return null
                if (candidateIdea.id == target.id) {
                    return null
                }
                if (candidateIdea.teamId == target.teamId) {
                    DuplicateAnalysisMatch.ownCandidateIdea(
                        sourceId = sourceId,
                        sourceTeamId = candidateIdea.teamId,
                        sourceTitle = candidateIdea.title,
                        similarityLevel = similarityLevel,
                        overlapDimensions = dimensions,
                        overlapSummary = normalizedSummary(overlapSummary),
                    )
                } else {
                    DuplicateAnalysisMatch.redactedPrivateCandidateIdea(
                        similarityLevel = similarityLevel,
                        overlapDimensions = dimensions,
                        overlapSummary = "Another team's private candidate idea has non-identifying overlap.",
                    )
                }
            }
        }
    }

    private fun normalizedSummary(value: String?): String {
        return value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.take(MAX_OVERLAP_SUMMARY_LENGTH)
            ?: "The source overlaps with the target candidate idea."
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?): T? {
        return value
            ?.trim()
            ?.uppercase()
            ?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
    }

    private fun SimilarityLevel.rank(): Int {
        return when (this) {
            SimilarityLevel.LOW -> 1
            SimilarityLevel.MEDIUM -> 2
            SimilarityLevel.HIGH -> 3
        }
    }

    private companion object {
        const val MAX_OVERLAP_SUMMARY_LENGTH = 300
    }
}

private fun CandidateIdea.toPromptCandidate(teamScope: TeamScope): GeminiPromptCandidateIdea {
    return GeminiPromptCandidateIdea(
        sourceId = requireNotNull(id).value,
        teamScope = teamScope.name,
        title = title,
        summary = summary,
        problem = problem,
        targetUsers = targetUsers,
        solution = solution,
        category = category.name,
        platforms = platforms.map { it.name },
    )
}

private fun DuplicateReleasedServiceInput.toPromptReleasedService(): GeminiPromptReleasedService? {
    val serviceProfileId = serviceProfile.id ?: return null
    return GeminiPromptReleasedService(
        sourceId = serviceProfileId,
        title = serviceProfile.name,
        summary = serviceProfile.summary,
        description = serviceProfile.description,
        category = serviceProfile.category.name,
        platforms = serviceProfile.platforms.map { it.name },
        openCampaignDescriptions = openCampaignDescriptions,
    )
}

private enum class TeamScope {
    OWN_TEAM,
    OTHER_TEAM,
}

private data class GeminiPromptCandidateIdea(
    val sourceId: Long,
    val teamScope: String,
    val title: String,
    val summary: String,
    val problem: String,
    val targetUsers: String,
    val solution: String,
    val category: String,
    val platforms: List<String>,
)

private data class GeminiPromptReleasedService(
    val sourceId: Long,
    val title: String,
    val summary: String,
    val description: String,
    val category: String,
    val platforms: List<String>,
    val openCampaignDescriptions: List<String>,
)

private data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig,
) {
    companion object {
        fun forPrompt(
            prompt: String,
            properties: GeminiDuplicateAnalysisProperties,
        ): GeminiGenerateContentRequest {
            return GeminiGenerateContentRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt)),
                    ),
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseJsonSchema = GeminiDuplicateAnalysisResponseSchema.value,
                    temperature = properties.temperature,
                    maxOutputTokens = properties.maxOutputTokens,
                ),
            )
        }
    }
}

private data class GeminiContent(
    val parts: List<GeminiPart>,
)

private data class GeminiPart(
    val text: String?,
)

private data class GeminiGenerationConfig(
    val responseMimeType: String,
    val responseJsonSchema: Map<String, Any>,
    val temperature: Double,
    val maxOutputTokens: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiCandidate(
    val content: GeminiResponseContent? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiResponseContent(
    val parts: List<GeminiResponsePart> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GeminiResponsePart(
    val text: String? = null,
)

private data class GeminiDuplicateAnalysisResponse(
    val matches: List<GeminiDuplicateAnalysisMatch> = emptyList(),
)

private data class GeminiDuplicateAnalysisMatch(
    val sourceType: String? = null,
    val sourceId: Long? = null,
    val similarityLevel: String? = null,
    val overlapDimensions: List<String> = emptyList(),
    val overlapSummary: String? = null,
)

private object GeminiDuplicateAnalysisResponseSchema {
    val value: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "matches" to mapOf(
                "type" to "array",
                "items" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "sourceType" to mapOf(
                            "type" to "string",
                            "enum" to listOf("RELEASED_SERVICE", "PRIVATE_CANDIDATE_IDEA"),
                        ),
                        "sourceId" to mapOf("type" to "integer"),
                        "similarityLevel" to mapOf(
                            "type" to "string",
                            "enum" to listOf("LOW", "MEDIUM", "HIGH"),
                        ),
                        "overlapDimensions" to mapOf(
                            "type" to "array",
                            "items" to mapOf(
                                "type" to "string",
                                "enum" to listOf(
                                    "PROBLEM",
                                    "TARGET_USER",
                                    "SOLUTION",
                                    "FEATURE",
                                    "PLATFORM",
                                    "BUSINESS_MODEL",
                                ),
                            ),
                        ),
                        "overlapSummary" to mapOf(
                            "type" to "string",
                            "description" to "Short non-identifying explanation of the overlap.",
                        ),
                    ),
                    "required" to listOf(
                        "sourceType",
                        "sourceId",
                        "similarityLevel",
                        "overlapDimensions",
                        "overlapSummary",
                    ),
                ),
            ),
        ),
        "required" to listOf("matches"),
    )
}
