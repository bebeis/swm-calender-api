package swm.calender.client.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.hamcrest.Matchers.containsString
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import swm.calender.core.common.id.CandidateIdeaId
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.DuplicateAnalysisSourceType
import swm.calender.core.enums.OverlapDimension
import swm.calender.core.enums.Platform
import swm.calender.core.enums.SimilarityLevel
import swm.calender.core.enums.SourceDisclosure
import swm.calender.match.domain.model.CandidateIdea
import swm.calender.match.domain.model.ServiceProfile
import swm.calender.match.exception.DuplicateIdeaAnalyzerException
import swm.calender.match.exception.MatchErrorMessage
import swm.calender.match.implement.DuplicateCandidateIdeaInput
import swm.calender.match.implement.DuplicateIdeaAnalysisRequest
import swm.calender.match.implement.DuplicateReleasedServiceInput
import java.time.Instant

class GeminiDuplicateIdeaAnalyzerTest :
    FunSpec({
        lateinit var server: MockRestServiceServer
        lateinit var analyzer: GeminiDuplicateIdeaAnalyzer

        beforeTest {
            val restClientBuilder = RestClient.builder()
            server = MockRestServiceServer.bindTo(restClientBuilder).build()
            analyzer = GeminiDuplicateIdeaAnalyzer(
                properties = GeminiDuplicateAnalysisProperties(apiKey = "test-key"),
                restClientBuilder = restClientBuilder,
                objectMapper = objectMapper,
            )
        }

        afterTest {
            server.verify()
        }

        test("analyze maps Gemini structured output and redacts other team private candidates") {
            // given
            val target = candidateIdea(
                id = CandidateIdeaId(1L),
                teamId = TeamId(1L),
                title = "Study helper",
                problem = "students lose study plans",
                solution = "automated study planning",
            )
            val otherTeamCandidate = candidateIdea(
                id = CandidateIdeaId(2L),
                teamId = TeamId(2L),
                title = "Private competitor",
                problem = "students lose study plans",
                solution = "automated study planning",
            )
            val releasedService = serviceProfile(id = 11L, teamId = TeamId(3L))
            val geminiJson = """
                {
                  "matches": [
                    {
                      "sourceType": "PRIVATE_CANDIDATE_IDEA",
                      "sourceId": 2,
                      "similarityLevel": "HIGH",
                      "overlapDimensions": ["PROBLEM", "SOLUTION"],
                      "overlapSummary": "This should not be trusted for private sources."
                    },
                    {
                      "sourceType": "RELEASED_SERVICE",
                      "sourceId": 11,
                      "similarityLevel": "MEDIUM",
                      "overlapDimensions": ["TARGET_USER", "PLATFORM"],
                      "overlapSummary": "Public service targets the same student workflow."
                    }
                  ]
                }
            """.trimIndent()
            server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().string(containsString("responseJsonSchema")))
                .andRespond(withSuccess(geminiResponse(geminiJson), MediaType.APPLICATION_JSON))

            // when
            val matches = analyzer.analyze(
                DuplicateIdeaAnalysisRequest(
                    candidateIdea = target,
                    releasedServices = listOf(
                        DuplicateReleasedServiceInput(
                            serviceProfile = releasedService,
                            openCampaignDescriptions = listOf("Find study planning testers."),
                        ),
                    ),
                    privateCandidateIdeas = listOf(
                        DuplicateCandidateIdeaInput(target),
                        DuplicateCandidateIdeaInput(otherTeamCandidate),
                    ),
                ),
            )

            // then
            matches shouldHaveSize 2
            val privateMatch = matches.first { it.sourceType == DuplicateAnalysisSourceType.PRIVATE_CANDIDATE_IDEA }
            privateMatch.sourceDisclosure shouldBe SourceDisclosure.REDACTED
            privateMatch.sourceId shouldBe null
            privateMatch.sourceTeamId shouldBe null
            privateMatch.sourceTitle shouldBe null
            privateMatch.similarityLevel shouldBe SimilarityLevel.HIGH
            privateMatch.overlapDimensions shouldBe listOf(OverlapDimension.PROBLEM, OverlapDimension.SOLUTION)

            val releasedMatch = matches.first { it.sourceType == DuplicateAnalysisSourceType.RELEASED_SERVICE }
            releasedMatch.sourceDisclosure shouldBe SourceDisclosure.PUBLIC
            releasedMatch.sourceId shouldBe 11L
            releasedMatch.sourceTeamId shouldBe TeamId(3L)
            releasedMatch.sourceTitle shouldBe "Released study tool"
        }

        test("analyze fails with safe message when api key is missing") {
            // given
            val analyzerWithoutKey = GeminiDuplicateIdeaAnalyzer(
                properties = GeminiDuplicateAnalysisProperties(apiKey = ""),
                restClientBuilder = RestClient.builder(),
                objectMapper = objectMapper,
            )

            // when & then
            val exception = shouldThrow<DuplicateIdeaAnalyzerException> {
                analyzerWithoutKey.analyze(
                    DuplicateIdeaAnalysisRequest(
                        candidateIdea = candidateIdea(CandidateIdeaId(1L), TeamId(1L)),
                        releasedServices = emptyList(),
                        privateCandidateIdeas = emptyList(),
                    ),
                )
            }
            exception.errorMessage shouldBe MatchErrorMessage.DUPLICATE_ANALYSIS_PROVIDER_API_KEY_REQUIRED
        }
    }) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
        private val baseInstant = Instant.parse("2026-05-10T00:00:00Z")

        private fun geminiResponse(jsonText: String): String {
            return objectMapper.writeValueAsString(
                mapOf(
                    "candidates" to listOf(
                        mapOf(
                            "content" to mapOf(
                                "parts" to listOf(
                                    mapOf("text" to jsonText),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        private fun candidateIdea(
            id: CandidateIdeaId,
            teamId: TeamId,
            title: String = "Study helper",
            problem: String = "students lose study plans",
            solution: String = "automated study planning",
        ): CandidateIdea {
            return CandidateIdea.createPrivate(
                teamId = teamId,
                title = title,
                summary = "Planning support for students",
                problem = problem,
                targetUsers = "students",
                solution = solution,
                category = CampaignCategory.EDUCATION,
                platforms = listOf(Platform.WEB),
                createdByUserId = UserId(10L),
                createdAt = baseInstant,
            ).copy(id = id)
        }

        private fun serviceProfile(
            id: Long,
            teamId: TeamId,
        ): ServiceProfile {
            return ServiceProfile.createActive(
                teamId = teamId,
                nextVersion = 1,
                isPublic = true,
                name = "Released study tool",
                summary = "Study planning for students",
                description = "Automates student study plans and reminders.",
                category = CampaignCategory.EDUCATION,
                platforms = listOf(Platform.WEB),
                screenshotUrls = emptyList(),
                demoUrl = null,
                createdAt = baseInstant,
            ).copy(id = id)
        }
    }
}
