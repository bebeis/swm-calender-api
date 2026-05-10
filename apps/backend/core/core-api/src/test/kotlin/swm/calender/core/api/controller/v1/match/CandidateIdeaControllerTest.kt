package swm.calender.core.api.controller.v1.match

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import jakarta.validation.Validation
import org.springframework.http.MediaType
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import swm.calender.core.api.controller.v1.match.service.CandidateIdeaApiFacade
import swm.calender.core.api.controller.v1.match.service.MatchRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
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
import swm.calender.match.service.response.CandidateIdeaResponse
import swm.calender.match.service.response.DuplicateAnalysisMatchResponse
import swm.calender.match.service.response.DuplicateAnalysisResponse
import java.time.Instant

class CandidateIdeaControllerTest : FunSpec() {
    private val restDocumentation = ManualRestDocumentation()
    private val validatorFactory = Validation.buildDefaultValidatorFactory()

    private lateinit var candidateIdeaService: CandidateIdeaService
    private lateinit var duplicateAnalysisService: DuplicateAnalysisService
    private lateinit var mockMvc: MockMvc

    init {
        beforeTest { testCase ->
            restDocumentation.beforeTest(javaClass, testCase.name.name)
            candidateIdeaService = mockk()
            duplicateAnalysisService = mockk()

            val controller = CandidateIdeaController(
                candidateIdeaApiFacade = CandidateIdeaApiFacade(
                    candidateIdeaService = candidateIdeaService,
                    duplicateAnalysisService = duplicateAnalysisService,
                ),
                matchRequestValidator = MatchRequestValidator(validatorFactory.validator),
            )

            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .apply<StandaloneMockMvcBuilder>(
                    MockMvcRestDocumentation.documentationConfiguration(restDocumentation),
                )
                .build()
        }

        afterTest {
            restDocumentation.afterTest()
        }

        afterSpec {
            validatorFactory.close()
        }

        test("create candidate idea returns private idea and documents the endpoint") {
            // given
            every { candidateIdeaService.createCandidateIdea(any()) } returns candidateIdeaResponse()

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post(
                    "/api/v1/match/candidate-ideas",
                )
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title": "Study helper",
                          "summary": "Planning support",
                          "problem": "Students lose study plan",
                          "targetUsers": "Students",
                          "solution": "Study plan automation",
                          "category": "EDUCATION",
                          "platforms": ["WEB"]
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.candidateIdeaId").value(31L))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andDo(
                    document(
                        "match-candidate-idea-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("title").type(JsonFieldType.STRING).description("Idea title"),
                            fieldWithPath("summary").type(JsonFieldType.STRING).description("Idea summary"),
                            fieldWithPath("problem").type(JsonFieldType.STRING).description("Problem statement"),
                            fieldWithPath("targetUsers").type(JsonFieldType.STRING).description("Target users"),
                            fieldWithPath("solution").type(JsonFieldType.STRING).description("Solution summary"),
                            fieldWithPath("category").type(JsonFieldType.STRING).description("Category"),
                            fieldWithPath("platforms").type(JsonFieldType.ARRAY).description("Platforms"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.candidateIdeaId").type(JsonFieldType.NUMBER)
                                .description("Candidate idea id"),
                            fieldWithPath("data.teamId").type(JsonFieldType.NUMBER).description("Team id"),
                            fieldWithPath("data.title").type(JsonFieldType.STRING).description("Idea title"),
                            fieldWithPath("data.summary").type(JsonFieldType.STRING).description("Idea summary"),
                            fieldWithPath("data.problem").type(JsonFieldType.STRING)
                                .description("Problem statement"),
                            fieldWithPath("data.targetUsers").type(JsonFieldType.STRING)
                                .description("Target users"),
                            fieldWithPath("data.solution").type(JsonFieldType.STRING)
                                .description("Solution summary"),
                            fieldWithPath("data.category").type(JsonFieldType.STRING).description("Category"),
                            fieldWithPath("data.platforms").type(JsonFieldType.ARRAY).description("Platforms"),
                            fieldWithPath("data.visibility").type(JsonFieldType.STRING).description("Visibility"),
                            fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("Created time"),
                            fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("Updated time"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("list candidate ideas returns only the authenticated team's private ideas") {
            // given
            every { candidateIdeaService.listCandidateIdeas(UserId(1L)) } returns listOf(candidateIdeaResponse())

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get(
                    "/api/v1/match/candidate-ideas",
                )
                    .principal(authenticatedUser()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].candidateIdeaId").value(31L))
                .andDo(
                    document(
                        "match-candidate-ideas-list",
                        preprocessResponse(prettyPrint()),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.items").type(JsonFieldType.ARRAY)
                                .description("Private candidate ideas"),
                            fieldWithPath("data.items[].candidateIdeaId").type(JsonFieldType.NUMBER)
                                .description("Candidate idea id"),
                            fieldWithPath("data.items[].teamId").type(JsonFieldType.NUMBER)
                                .description("Team id"),
                            fieldWithPath("data.items[].title").type(JsonFieldType.STRING)
                                .description("Idea title"),
                            fieldWithPath("data.items[].summary").type(JsonFieldType.STRING)
                                .description("Idea summary"),
                            fieldWithPath("data.items[].problem").type(JsonFieldType.STRING)
                                .description("Problem statement"),
                            fieldWithPath("data.items[].targetUsers").type(JsonFieldType.STRING)
                                .description("Target users"),
                            fieldWithPath("data.items[].solution").type(JsonFieldType.STRING)
                                .description("Solution summary"),
                            fieldWithPath("data.items[].category").type(JsonFieldType.STRING)
                                .description("Category"),
                            fieldWithPath("data.items[].platforms").type(JsonFieldType.ARRAY)
                                .description("Platforms"),
                            fieldWithPath("data.items[].visibility").type(JsonFieldType.STRING)
                                .description("Visibility"),
                            fieldWithPath("data.items[].createdAt").type(JsonFieldType.STRING)
                                .description("Created time"),
                            fieldWithPath("data.items[].updatedAt").type(JsonFieldType.STRING)
                                .description("Updated time"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("run duplicate analysis returns redacted private matches and documents the endpoint") {
            // given
            every { duplicateAnalysisService.runDuplicateAnalysis(any()) } returns DuplicateAnalysisResponse(
                analysisId = 41L,
                candidateIdeaId = 31L,
                status = DuplicateAnalysisStatus.COMPLETED,
                scannedReleasedServiceCount = 1,
                scannedCandidateIdeaCount = 2,
                matches = listOf(
                    DuplicateAnalysisMatchResponse(
                        sourceType = DuplicateAnalysisSourceType.PRIVATE_CANDIDATE_IDEA,
                        sourceDisclosure = SourceDisclosure.REDACTED,
                        sourceId = null,
                        sourceTeamId = null,
                        sourceTitle = null,
                        similarityLevel = SimilarityLevel.HIGH,
                        overlapDimensions = listOf(OverlapDimension.PROBLEM, OverlapDimension.SOLUTION),
                        overlapSummary = "Another team's private candidate idea has non-identifying overlap.",
                    ),
                ),
                failureReason = null,
                generatedAt = Instant.parse("2026-05-10T00:00:00Z"),
            )

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post(
                    "/api/v1/match/candidate-ideas/{candidateIdeaId}/duplicate-analysis",
                    31L,
                )
                    .principal(authenticatedUser()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.analysisId").value(41L))
                .andExpect(jsonPath("$.data.matches[0].sourceDisclosure").value("REDACTED"))
                .andExpect(jsonPath("$.data.matches[0].sourceId").doesNotExist())
                .andDo(
                    document(
                        "match-candidate-idea-duplicate-analysis-run",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("candidateIdeaId").description("Candidate idea id"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.analysisId").type(JsonFieldType.NUMBER)
                                .description("Duplicate analysis id"),
                            fieldWithPath("data.candidateIdeaId").type(JsonFieldType.NUMBER)
                                .description("Candidate idea id"),
                            fieldWithPath("data.status").type(JsonFieldType.STRING)
                                .description("Analysis status"),
                            fieldWithPath("data.scannedReleasedServiceCount").type(JsonFieldType.NUMBER)
                                .description("Scanned released service count"),
                            fieldWithPath("data.scannedCandidateIdeaCount").type(JsonFieldType.NUMBER)
                                .description("Scanned candidate idea count"),
                            fieldWithPath("data.matches").type(JsonFieldType.ARRAY).description("Matches"),
                            fieldWithPath("data.matches[].sourceType").type(JsonFieldType.STRING)
                                .description("Source type"),
                            fieldWithPath("data.matches[].sourceDisclosure").type(JsonFieldType.STRING)
                                .description("Disclosure level"),
                            fieldWithPath("data.matches[].sourceId").type(JsonFieldType.VARIES)
                                .description("Source id when visible").optional(),
                            fieldWithPath("data.matches[].sourceTeamId").type(JsonFieldType.VARIES)
                                .description("Source team id when visible").optional(),
                            fieldWithPath("data.matches[].sourceTitle").type(JsonFieldType.VARIES)
                                .description("Source title when visible").optional(),
                            fieldWithPath("data.matches[].similarityLevel").type(JsonFieldType.STRING)
                                .description("Similarity level"),
                            fieldWithPath("data.matches[].overlapDimensions").type(JsonFieldType.ARRAY)
                                .description("Overlap dimensions"),
                            fieldWithPath("data.matches[].overlapSummary").type(JsonFieldType.STRING)
                                .description("Redacted-safe overlap summary"),
                            fieldWithPath("data.failureReason").type(JsonFieldType.VARIES)
                                .description("Failure reason").optional(),
                            fieldWithPath("data.generatedAt").type(JsonFieldType.STRING)
                                .description("Generated time"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }
    }

    private fun candidateIdeaResponse(): CandidateIdeaResponse {
        return CandidateIdeaResponse(
            candidateIdeaId = 31L,
            teamId = TeamId(1L),
            title = "Study helper",
            summary = "Planning support",
            problem = "Students lose study plan",
            targetUsers = "Students",
            solution = "Study plan automation",
            category = CampaignCategory.EDUCATION,
            platforms = listOf(Platform.WEB),
            visibility = CandidateIdeaVisibility.PRIVATE,
            createdAt = Instant.parse("2026-05-10T00:00:00Z"),
            updatedAt = Instant.parse("2026-05-10T00:00:00Z"),
        )
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            AuthenticatedUser(
                userId = UserId(1L),
                email = "owner@swm.app",
                name = "Owner",
            ),
            null,
            emptyList(),
        )
    }
}
