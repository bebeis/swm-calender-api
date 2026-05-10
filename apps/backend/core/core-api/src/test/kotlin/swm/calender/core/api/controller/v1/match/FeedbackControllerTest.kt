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
import swm.calender.core.api.controller.v1.match.service.FeedbackApiFacade
import swm.calender.core.api.controller.v1.match.service.MatchRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.match.service.FeedbackService
import swm.calender.match.service.response.FeedbackResponse
import swm.calender.match.service.response.FeedbackScoresResponse
import swm.calender.match.service.response.TeamTestHistoryItemResponse
import swm.calender.match.service.response.TeamTestHistoryResponse
import java.time.Instant

class FeedbackControllerTest : FunSpec() {
    private val restDocumentation = ManualRestDocumentation()
    private val validatorFactory = Validation.buildDefaultValidatorFactory()

    private lateinit var feedbackService: FeedbackService
    private lateinit var mockMvc: MockMvc

    init {
        beforeTest { testCase ->
            restDocumentation.beforeTest(javaClass, testCase.name.name)
            feedbackService = mockk()

            val controller = FeedbackController(
                feedbackApiFacade = FeedbackApiFacade(feedbackService),
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

        test("submit feedback stores structured feedback and documents endpoint") {
            // given
            every { feedbackService.submitFeedback(any()) } returns feedbackResponse()

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post(
                    "/api/v1/match/assignments/{assignmentId}/feedback",
                    31L,
                )
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "scores": {
                            "usability": 5,
                            "value": 4,
                            "reliability": 5,
                            "recommendation": 4
                          },
                          "summary": "The service was useful during testing.",
                          "improvementSuggestion": "Add onboarding."
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.feedbackId").value(41L))
                .andExpect(jsonPath("$.data.scores.usability").value(5))
                .andDo(
                    document(
                        "match-feedback-submit",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("assignmentId").description("Assignment id"),
                        ),
                        requestFields(
                            fieldWithPath("scores.usability").type(JsonFieldType.NUMBER)
                                .description("Usability score from 1 to 5"),
                            fieldWithPath("scores.value").type(JsonFieldType.NUMBER)
                                .description("Value score from 1 to 5"),
                            fieldWithPath("scores.reliability").type(JsonFieldType.NUMBER)
                                .description("Reliability score from 1 to 5"),
                            fieldWithPath("scores.recommendation").type(JsonFieldType.NUMBER)
                                .description("Recommendation score from 1 to 5"),
                            fieldWithPath("summary").type(JsonFieldType.STRING)
                                .description("Feedback summary"),
                            fieldWithPath("improvementSuggestion").type(JsonFieldType.STRING)
                                .description("Optional improvement suggestion").optional(),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.feedbackId").type(JsonFieldType.NUMBER)
                                .description("Feedback id"),
                            fieldWithPath("data.assignmentId").type(JsonFieldType.NUMBER)
                                .description("Assignment id"),
                            fieldWithPath("data.submittedByTeamId").type(JsonFieldType.NUMBER)
                                .description("Submitting tester team id"),
                            fieldWithPath("data.scores.usability").type(JsonFieldType.NUMBER)
                                .description("Usability score"),
                            fieldWithPath("data.scores.value").type(JsonFieldType.NUMBER)
                                .description("Value score"),
                            fieldWithPath("data.scores.reliability").type(JsonFieldType.NUMBER)
                                .description("Reliability score"),
                            fieldWithPath("data.scores.recommendation").type(JsonFieldType.NUMBER)
                                .description("Recommendation score"),
                            fieldWithPath("data.summary").type(JsonFieldType.STRING)
                                .description("Feedback summary"),
                            fieldWithPath("data.improvementSuggestion").type(JsonFieldType.STRING)
                                .description("Improvement suggestion").optional(),
                            fieldWithPath("data.submittedAt").type(JsonFieldType.STRING)
                                .description("Submission time"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("get team test history returns assignment and feedback summary") {
            // given
            every { feedbackService.getTeamTestHistory(any()) } returns TeamTestHistoryResponse(
                items = listOf(
                    TeamTestHistoryItemResponse(
                        assignmentId = 31L,
                        campaignId = 21L,
                        serviceName = "Service",
                        testerTeamId = TeamId(1L),
                        targetTeamId = TeamId(2L),
                        assignmentStatus = AssignmentStatus.FEEDBACK_SUBMITTED,
                        feedbackSubmittedAt = Instant.parse("2026-05-10T00:00:00Z"),
                        feedbackSummary = "The service was useful during testing.",
                    ),
                ),
            )

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get(
                    "/api/v1/match/test-history",
                )
                    .principal(authenticatedUser()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items[0].assignmentId").value(31L))
                .andExpect(jsonPath("$.data.items[0].feedbackSummary").value("The service was useful during testing."))
                .andDo(
                    document(
                        "match-test-history-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.items").type(JsonFieldType.ARRAY)
                                .description("Team test history items"),
                            fieldWithPath("data.items[].assignmentId").type(JsonFieldType.NUMBER)
                                .description("Assignment id"),
                            fieldWithPath("data.items[].campaignId").type(JsonFieldType.NUMBER)
                                .description("Campaign id"),
                            fieldWithPath("data.items[].serviceName").type(JsonFieldType.STRING)
                                .description("Service name"),
                            fieldWithPath("data.items[].testerTeamId").type(JsonFieldType.NUMBER)
                                .description("Tester team id"),
                            fieldWithPath("data.items[].targetTeamId").type(JsonFieldType.NUMBER)
                                .description("Target team id"),
                            fieldWithPath("data.items[].assignmentStatus").type(JsonFieldType.STRING)
                                .description("Assignment status"),
                            fieldWithPath("data.items[].feedbackSubmittedAt").type(JsonFieldType.STRING)
                                .description("Feedback submission time").optional(),
                            fieldWithPath("data.items[].feedbackSummary").type(JsonFieldType.STRING)
                                .description("Feedback summary").optional(),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }
    }

    private fun feedbackResponse(): FeedbackResponse {
        return FeedbackResponse(
            feedbackId = 41L,
            assignmentId = 31L,
            submittedByTeamId = TeamId(1L),
            scores = FeedbackScoresResponse(
                usability = 5,
                value = 4,
                reliability = 5,
                recommendation = 4,
            ),
            summary = "The service was useful during testing.",
            improvementSuggestion = "Add onboarding.",
            submittedAt = Instant.parse("2026-05-10T00:00:00Z"),
        )
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            AuthenticatedUser(
                userId = UserId(1L),
                email = "tester@swm.app",
                name = "Tester",
            ),
            null,
            emptyList(),
        )
    }
}
