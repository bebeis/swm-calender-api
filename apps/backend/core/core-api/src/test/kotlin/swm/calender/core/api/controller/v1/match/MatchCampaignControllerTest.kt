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
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import swm.calender.core.api.controller.v1.match.service.MatchCampaignApiFacade
import swm.calender.core.api.controller.v1.match.service.MatchRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.CampaignCategory
import swm.calender.core.enums.CampaignStatus
import swm.calender.core.enums.Platform
import swm.calender.match.service.MatchCampaignService
import swm.calender.match.service.response.CampaignResponse
import swm.calender.match.service.response.CampaignSearchItemResponse
import swm.calender.match.service.response.ServiceProfileResponse
import java.time.OffsetDateTime

class MatchCampaignControllerTest : FunSpec() {
    private val restDocumentation = ManualRestDocumentation()
    private val validatorFactory = Validation.buildDefaultValidatorFactory()

    private lateinit var matchCampaignService: MatchCampaignService
    private lateinit var mockMvc: MockMvc

    init {
        beforeTest { testCase ->
            restDocumentation.beforeTest(javaClass, testCase.name.name)
            matchCampaignService = mockk()

            val controller = MatchCampaignController(
                matchCampaignApiFacade = MatchCampaignApiFacade(matchCampaignService),
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

        test("create service profile returns profile summary and documents the endpoint") {
            // given
            every { matchCampaignService.createServiceProfile(any()) } returns ServiceProfileResponse(
                serviceProfileId = 11L,
                teamId = TeamId(1L),
                active = true,
                isPublic = true,
                name = "SWM Teams",
                summary = "Team workflow",
                category = CampaignCategory.PRODUCTIVITY,
                platforms = listOf(Platform.WEB),
            )

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post(
                    "/api/v1/match/service-profiles",
                )
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "SWM Teams",
                          "summary": "Team workflow",
                          "description": "Team workflow for SWM teams",
                          "category": "PRODUCTIVITY",
                          "platforms": ["WEB"],
                          "screenshotUrls": ["https://example.com/screenshot.png"],
                          "demoUrl": "https://example.com",
                          "public": true
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.serviceProfileId").value(11L))
                .andExpect(jsonPath("$.data.public").value(true))
                .andDo(
                    document(
                        "match-service-profile-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("name").type(JsonFieldType.STRING).description("Service name"),
                            fieldWithPath("summary").type(JsonFieldType.STRING).description("Short service summary"),
                            fieldWithPath("description").type(JsonFieldType.STRING)
                                .description("Detailed service description"),
                            fieldWithPath("category").type(JsonFieldType.STRING).description("Campaign category"),
                            fieldWithPath("platforms").type(JsonFieldType.ARRAY).description("Supported platforms"),
                            fieldWithPath("screenshotUrls").type(JsonFieldType.ARRAY)
                                .description("Public screenshot URLs").optional(),
                            fieldWithPath("demoUrl").type(JsonFieldType.STRING).description("Demo URL").optional(),
                            fieldWithPath("public").type(JsonFieldType.BOOLEAN).description("Public visibility"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.serviceProfileId").type(JsonFieldType.NUMBER)
                                .description("Service profile id"),
                            fieldWithPath("data.teamId").type(JsonFieldType.NUMBER).description("Team id"),
                            fieldWithPath("data.active").type(JsonFieldType.BOOLEAN).description("Active flag"),
                            fieldWithPath("data.public").type(JsonFieldType.BOOLEAN).description("Public flag"),
                            fieldWithPath("data.name").type(JsonFieldType.STRING).description("Service name"),
                            fieldWithPath("data.summary").type(JsonFieldType.STRING).description("Service summary"),
                            fieldWithPath("data.category").type(JsonFieldType.STRING).description("Category"),
                            fieldWithPath("data.platforms").type(JsonFieldType.ARRAY).description("Platforms"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("search campaigns returns filtered public campaigns and documents query parameters") {
            // given
            every { matchCampaignService.searchCampaigns(any()) } returns listOf(
                CampaignSearchItemResponse(
                    campaignId = 21L,
                    teamId = TeamId(1L),
                    teamName = "Team A",
                    serviceName = "SWM Teams",
                    serviceSummary = "Team workflow",
                    category = CampaignCategory.PRODUCTIVITY,
                    platforms = listOf(Platform.WEB),
                    reciprocalAvailable = true,
                    deadline = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
                    status = CampaignStatus.OPEN,
                ),
            )

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get("/api/v1/match/campaigns")
                    .principal(authenticatedUser())
                    .queryParam("category", "PRODUCTIVITY")
                    .queryParam("platform", "WEB")
                    .queryParam("reciprocalAvailable", "true")
                    .queryParam("sort", "deadline"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].campaignId").value(21L))
                .andDo(
                    document(
                        "match-campaigns-search",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                            parameterWithName("category").description("Optional campaign category").optional(),
                            parameterWithName("platform").description("Optional platform").optional(),
                            parameterWithName("reciprocalAvailable").description("Optional reciprocal filter")
                                .optional(),
                            parameterWithName("sort").description("Sort mode, latest or deadline").optional(),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("Campaigns"),
                            fieldWithPath("data.items[].campaignId").type(JsonFieldType.NUMBER)
                                .description("Campaign id"),
                            fieldWithPath("data.items[].teamId").type(JsonFieldType.NUMBER).description("Team id"),
                            fieldWithPath("data.items[].teamName").type(JsonFieldType.STRING)
                                .description("Team name"),
                            fieldWithPath("data.items[].serviceName").type(JsonFieldType.STRING)
                                .description("Service name"),
                            fieldWithPath("data.items[].serviceSummary").type(JsonFieldType.STRING)
                                .description("Service summary"),
                            fieldWithPath("data.items[].category").type(JsonFieldType.STRING)
                                .description("Category"),
                            fieldWithPath("data.items[].platforms").type(JsonFieldType.ARRAY)
                                .description("Platforms"),
                            fieldWithPath("data.items[].reciprocalAvailable").type(JsonFieldType.BOOLEAN)
                                .description("Reciprocal beta availability"),
                            fieldWithPath("data.items[].deadline").type(JsonFieldType.STRING)
                                .description("Campaign deadline"),
                            fieldWithPath("data.items[].status").type(JsonFieldType.STRING)
                                .description("Campaign status"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("create and change campaign status return campaign summaries") {
            // given
            every { matchCampaignService.createCampaign(any()) } returns campaignResponse(CampaignStatus.OPEN)
            every { matchCampaignService.changeCampaignStatus(any()) } returns campaignResponse(CampaignStatus.CLOSED)

            // when & then
            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post("/api/v1/match/campaigns")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "title": "Beta",
                          "description": "Try this service",
                          "targetTeamCount": 3,
                          "deadline": "2026-05-20T00:00:00Z",
                          "reciprocalAvailable": true,
                          "requirements": "Chrome"
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("OPEN"))

            mockMvc.perform(
                org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch(
                    "/api/v1/match/campaigns/{campaignId}/status",
                    21L,
                )
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"CLOSED"}"""),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
        }
    }

    private fun campaignResponse(status: CampaignStatus): CampaignResponse {
        return CampaignResponse(
            campaignId = 21L,
            serviceProfileId = 11L,
            title = "Beta",
            targetTeamCount = 3,
            deadline = OffsetDateTime.parse("2026-05-20T00:00:00Z"),
            reciprocalAvailable = true,
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
