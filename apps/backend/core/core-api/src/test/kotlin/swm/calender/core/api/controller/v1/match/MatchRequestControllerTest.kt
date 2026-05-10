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
import swm.calender.core.api.controller.v1.match.service.MatchRequestApiFacade
import swm.calender.core.api.controller.v1.match.service.MatchRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.AssignmentStatus
import swm.calender.core.enums.MatchRequestStatus
import swm.calender.core.enums.MatchRequestType
import swm.calender.match.service.MatchRequestService
import java.time.Instant
import swm.calender.match.service.response.AssignmentResponse as AssignmentServiceResponse
import swm.calender.match.service.response.FeedbackResponse as FeedbackServiceResponse
import swm.calender.match.service.response.FeedbackScoresResponse as FeedbackScoresServiceResponse
import swm.calender.match.service.response.MatchRequestResponse as MatchRequestServiceResponse
import swm.calender.match.service.response.MatchRequestStatusChangeResponse as MatchRequestStatusChangeServiceResponse

class MatchRequestControllerTest : FunSpec() {
    private val restDocumentation = ManualRestDocumentation()
    private val validatorFactory = Validation.buildDefaultValidatorFactory()

    private lateinit var matchRequestService: MatchRequestService
    private lateinit var mockMvc: MockMvc

    init {
        beforeTest { testCase ->
            restDocumentation.beforeTest(javaClass, testCase.name.name)
            matchRequestService = mockk()

            val controller = MatchRequestController(
                matchRequestApiFacade = MatchRequestApiFacade(matchRequestService),
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

        test("create match request returns pending request and documents endpoint") {
            // given
            every { matchRequestService.createRequest(any()) } returns matchRequestResponse(MatchRequestStatus.PENDING)

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post(
                    "/api/v1/match/campaigns/{campaignId}/requests",
                    21L,
                )
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "type": "ONE_WAY",
                          "message": "We can test this week."
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.requestId").value(11L))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andDo(
                    document(
                        "match-request-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("campaignId").description("Target campaign id"),
                        ),
                        requestFields(
                            fieldWithPath("type").type(JsonFieldType.STRING).description("Request type"),
                            fieldWithPath("message").type(JsonFieldType.STRING).description("Optional message")
                                .optional(),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.requestId").type(JsonFieldType.NUMBER)
                                .description("Match request id"),
                            fieldWithPath("data.campaignId").type(JsonFieldType.NUMBER)
                                .description("Campaign id"),
                            fieldWithPath("data.requestingTeamId").type(JsonFieldType.NUMBER)
                                .description("Requesting team id"),
                            fieldWithPath("data.targetTeamId").type(JsonFieldType.NUMBER)
                                .description("Target team id"),
                            fieldWithPath("data.type").type(JsonFieldType.STRING).description("Request type"),
                            fieldWithPath("data.status").type(JsonFieldType.STRING).description("Request status"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("change request status accepts request and documents assignment creation") {
            // given
            every { matchRequestService.changeRequestStatus(any()) } returns MatchRequestStatusChangeServiceResponse(
                request = matchRequestResponse(MatchRequestStatus.ACCEPTED),
                assignmentId = 31L,
                assignmentCreated = true,
            )

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch(
                    "/api/v1/match/requests/{requestId}/status",
                    11L,
                )
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"ACCEPTED"}"""),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.request.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.assignmentCreated").value(true))
                .andExpect(jsonPath("$.data.assignmentId").value(31L))
                .andDo(
                    document(
                        "match-request-status-change",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("requestId").description("Match request id"),
                        ),
                        requestFields(
                            fieldWithPath("status").type(JsonFieldType.STRING)
                                .description("Next status: ACCEPTED, REJECTED, or CANCELED"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.request.requestId").type(JsonFieldType.NUMBER)
                                .description("Match request id"),
                            fieldWithPath("data.request.campaignId").type(JsonFieldType.NUMBER)
                                .description("Campaign id"),
                            fieldWithPath("data.request.requestingTeamId").type(JsonFieldType.NUMBER)
                                .description("Requesting team id"),
                            fieldWithPath("data.request.targetTeamId").type(JsonFieldType.NUMBER)
                                .description("Target team id"),
                            fieldWithPath("data.request.type").type(JsonFieldType.STRING)
                                .description("Request type"),
                            fieldWithPath("data.request.status").type(JsonFieldType.STRING)
                                .description("Request status"),
                            fieldWithPath("data.assignmentId").type(JsonFieldType.NUMBER)
                                .description("Created assignment id").optional(),
                            fieldWithPath("data.assignmentCreated").type(JsonFieldType.BOOLEAN)
                                .description("Whether a new assignment was created"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("get assignment returns assignment detail with feedback") {
            // given
            every { matchRequestService.getAssignment(any()) } returns AssignmentServiceResponse(
                assignmentId = 31L,
                requestId = 11L,
                testerTeamId = TeamId(1L),
                targetTeamId = TeamId(2L),
                status = AssignmentStatus.FEEDBACK_SUBMITTED,
                feedback = FeedbackServiceResponse(
                    feedbackId = 41L,
                    assignmentId = 31L,
                    submittedByTeamId = TeamId(1L),
                    scores = FeedbackScoresServiceResponse(
                        usability = 5,
                        value = 4,
                        reliability = 5,
                        recommendation = 4,
                    ),
                    summary = "The service was useful during testing.",
                    improvementSuggestion = "Add onboarding.",
                    submittedAt = Instant.parse("2026-05-10T00:00:00Z"),
                ),
            )

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get(
                    "/api/v1/match/assignments/{assignmentId}",
                    31L,
                )
                    .principal(authenticatedUser()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.assignmentId").value(31L))
                .andExpect(jsonPath("$.data.status").value("FEEDBACK_SUBMITTED"))
                .andExpect(jsonPath("$.data.feedback.feedbackId").value(41L))
                .andDo(
                    document(
                        "match-assignment-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("assignmentId").description("Assignment id"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.assignmentId").type(JsonFieldType.NUMBER)
                                .description("Assignment id"),
                            fieldWithPath("data.requestId").type(JsonFieldType.NUMBER)
                                .description("Accepted request id"),
                            fieldWithPath("data.testerTeamId").type(JsonFieldType.NUMBER)
                                .description("Tester team id"),
                            fieldWithPath("data.targetTeamId").type(JsonFieldType.NUMBER)
                                .description("Target team id"),
                            fieldWithPath("data.status").type(JsonFieldType.STRING)
                                .description("Assignment status"),
                            fieldWithPath("data.feedback").type(JsonFieldType.OBJECT)
                                .description("Submitted feedback when available").optional(),
                            fieldWithPath("data.feedback.feedbackId").type(JsonFieldType.NUMBER)
                                .description("Feedback id").optional(),
                            fieldWithPath("data.feedback.assignmentId").type(JsonFieldType.NUMBER)
                                .description("Assignment id for the feedback").optional(),
                            fieldWithPath("data.feedback.submittedByTeamId").type(JsonFieldType.NUMBER)
                                .description("Submitting tester team id").optional(),
                            fieldWithPath("data.feedback.scores.usability").type(JsonFieldType.NUMBER)
                                .description("Usability score").optional(),
                            fieldWithPath("data.feedback.scores.value").type(JsonFieldType.NUMBER)
                                .description("Value score").optional(),
                            fieldWithPath("data.feedback.scores.reliability").type(JsonFieldType.NUMBER)
                                .description("Reliability score").optional(),
                            fieldWithPath("data.feedback.scores.recommendation").type(JsonFieldType.NUMBER)
                                .description("Recommendation score").optional(),
                            fieldWithPath("data.feedback.summary").type(JsonFieldType.STRING)
                                .description("Feedback summary").optional(),
                            fieldWithPath("data.feedback.improvementSuggestion").type(JsonFieldType.STRING)
                                .description("Improvement suggestion").optional(),
                            fieldWithPath("data.feedback.submittedAt").type(JsonFieldType.STRING)
                                .description("Feedback submission time").optional(),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }
    }

    private fun matchRequestResponse(status: MatchRequestStatus): MatchRequestServiceResponse {
        return MatchRequestServiceResponse(
            requestId = 11L,
            campaignId = 21L,
            requestingTeamId = TeamId(1L),
            targetTeamId = TeamId(2L),
            type = MatchRequestType.ONE_WAY,
            status = status,
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
