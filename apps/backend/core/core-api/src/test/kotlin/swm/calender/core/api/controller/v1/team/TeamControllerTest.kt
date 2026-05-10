package swm.calender.core.api.controller.v1.team

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.validation.Validation
import org.springframework.http.MediaType
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
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
import swm.calender.core.api.controller.v1.team.request.SubServiceActivationRequest
import swm.calender.core.api.controller.v1.team.request.TeamCreateRequest
import swm.calender.core.api.controller.v1.team.request.TeamJoinRequest
import swm.calender.core.api.controller.v1.team.request.TeamMemberRoleChangeRequest
import swm.calender.core.api.controller.v1.team.service.TeamApiFacade
import swm.calender.core.api.controller.v1.team.service.TeamRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.SubService
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.service.TeamService
import swm.calender.core.team.service.request.TeamSubServiceActivationRequest
import swm.calender.core.team.service.response.SubServiceActivationResponse as TeamServiceActivationResponse
import swm.calender.core.team.service.response.TeamMemberResponse as TeamServiceMemberResponse
import swm.calender.core.team.service.response.TeamResponse as TeamServiceResponse

class TeamControllerTest : FunSpec() {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val restDocumentation = ManualRestDocumentation()
    private val validatorFactory = Validation.buildDefaultValidatorFactory()

    private lateinit var teamService: TeamService
    private lateinit var mockMvc: MockMvc

    init {
        beforeTest { testCase ->
            restDocumentation.beforeTest(javaClass, testCase.name.name)
            teamService = mockk()

            val controller = TeamController(
                teamApiFacade = TeamApiFacade(teamService),
                teamRequestValidator = TeamRequestValidator(validatorFactory.validator),
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

        test("create team returns team envelope and documents the endpoint") {
            // given
            every { teamService.createTeam(any()) } returns teamResponse()

            // when & then
            mockMvc.perform(
                post("/api/v1/teams")
                    .principal(authenticatedUser(1L, "owner@swm.app", "Owner"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            TeamCreateRequest(
                                name = "Alpha Team",
                                description = "First MVP team",
                            ),
                        ),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.teamId").value(1L))
                .andExpect(jsonPath("$.data.name").value("Alpha Team"))
                .andExpect(jsonPath("$.data.inviteCode").value("TEAM-ALPHA"))
                .andDo(
                    document(
                        "teams-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("name").type(JsonFieldType.STRING).description("Team name"),
                            fieldWithPath("description").type(JsonFieldType.STRING).description("Team description")
                                .optional(),
                        ),
                        responseFields(
                            teamResponseFields(),
                        ),
                    ),
                )
        }

        test("join team and list members return API envelopes") {
            // given
            every { teamService.joinTeam(any()) } returns teamResponse()
            every { teamService.getMembers(TeamId(1L), UserId(2L)) } returns listOf(
                TeamServiceMemberResponse(
                    memberId = 1L,
                    userId = 1L,
                    name = "Owner",
                    email = "owner@swm.app",
                    role = TeamMemberRole.OWNER,
                ),
                TeamServiceMemberResponse(
                    memberId = 2L,
                    userId = 2L,
                    name = "Member",
                    email = "member@swm.app",
                    role = TeamMemberRole.MEMBER,
                ),
            )

            // when & then
            mockMvc.perform(
                post("/api/v1/teams/join")
                    .principal(authenticatedUser(2L, "member@swm.app", "Member"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(TeamJoinRequest(inviteCode = "TEAM-ALPHA"))),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.teamId").value(1L))
                .andDo(
                    document(
                        "teams-join",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("inviteCode").type(JsonFieldType.STRING).description("Team invite code"),
                        ),
                        responseFields(
                            teamResponseFields(),
                        ),
                    ),
                )

            mockMvc.perform(
                get("/api/v1/teams/{teamId}/members", 1L)
                    .principal(authenticatedUser(2L, "member@swm.app", "Member"))
                    .contentType(MediaType.APPLICATION_JSON),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].role").value("OWNER"))
                .andDo(
                    document(
                        "teams-members-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("teamId").description("Team id"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("Team members"),
                            fieldWithPath("data.items[].memberId").type(JsonFieldType.NUMBER)
                                .description("Team member id"),
                            fieldWithPath("data.items[].userId").type(JsonFieldType.NUMBER).description("User id"),
                            fieldWithPath("data.items[].name").type(JsonFieldType.STRING).description("Member name"),
                            fieldWithPath("data.items[].email").type(JsonFieldType.STRING).description("Member email"),
                            fieldWithPath("data.items[].role").type(JsonFieldType.STRING).description("Team role"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("owner can change a member role and toggle sub-services independently") {
            // given
            every {
                teamService.changeMemberRole(
                    teamId = TeamId(1L),
                    memberId = TeamMemberId(2L),
                    role = TeamMemberRole.OWNER,
                    actorUserId = UserId(1L),
                )
            } returns TeamServiceMemberResponse(
                memberId = 2L,
                userId = 2L,
                name = "Member",
                email = "member@swm.app",
                role = TeamMemberRole.OWNER,
            )
            every {
                teamService.changeSubServiceActivation(
                    TeamSubServiceActivationRequest(
                        teamId = TeamId(1L),
                        actorUserId = UserId(1L),
                        subService = SubService.CALENDAR,
                        enabled = true,
                    ),
                )
            } returns TeamServiceActivationResponse(
                teamId = 1L,
                calendarEnabled = true,
                matchEnabled = false,
            )

            // when & then
            mockMvc.perform(
                patch("/api/v1/teams/{teamId}/members/{memberId}/role", 1L, 2L)
                    .principal(authenticatedUser(1L, "owner@swm.app", "Owner"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            TeamMemberRoleChangeRequest(role = TeamMemberRole.OWNER),
                        ),
                    ),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.memberId").value(2L))
                .andExpect(jsonPath("$.data.role").value("OWNER"))
                .andDo(
                    document(
                        "teams-member-role-change",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("teamId").description("Team id"),
                            parameterWithName("memberId").description("Team member id"),
                        ),
                        requestFields(
                            fieldWithPath("role").type(JsonFieldType.STRING).description("Target team role"),
                        ),
                        responseFields(
                            memberResponseFields(),
                        ),
                    ),
                )

            mockMvc.perform(
                patch("/api/v1/teams/{teamId}/sub-services/{subService}", 1L, "calendar")
                    .principal(authenticatedUser(1L, "owner@swm.app", "Owner"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SubServiceActivationRequest(enabled = true))),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.calendarEnabled").value(true))
                .andExpect(jsonPath("$.data.matchEnabled").value(false))
                .andDo(
                    document(
                        "teams-sub-service-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("teamId").description("Team id"),
                            parameterWithName("subService").description("Sub-service name"),
                        ),
                        requestFields(
                            fieldWithPath("enabled").type(JsonFieldType.BOOLEAN)
                                .description("Sub-service enabled flag"),
                        ),
                        responseFields(
                            fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
                            fieldWithPath("data.teamId").type(JsonFieldType.NUMBER).description("Team id"),
                            fieldWithPath("data.calendarEnabled").type(JsonFieldType.BOOLEAN)
                                .description("Calendar activation status"),
                            fieldWithPath("data.matchEnabled").type(JsonFieldType.BOOLEAN)
                                .description("Match activation status"),
                            fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
                        ),
                    ),
                )
        }

        test("create team rejects invalid request bodies with a 400 envelope") {
            val response = mockMvc.perform(
                post("/api/v1/teams")
                    .principal(authenticatedUser(21L, "invalid@swm.app", "Invalid"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"","description":"invalid"}"""),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.result").value("ERROR"))
                .andExpect(jsonPath("$.error.data.reason").exists())
                .andReturn()

            response.response.contentAsString.contains("must not be blank") shouldBe true
        }
    }

    private fun authenticatedUser(
        userId: Long,
        email: String,
        name: String,
    ): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            AuthenticatedUser(
                userId = UserId(userId),
                email = email,
                name = name,
            ),
            null,
            emptyList(),
        )
    }

    private fun teamResponse(): TeamServiceResponse {
        return TeamServiceResponse(
            teamId = 1L,
            name = "Alpha Team",
            description = "First MVP team",
            inviteCode = "TEAM-ALPHA",
            calendarEnabled = false,
            matchEnabled = false,
        )
    }

    private fun teamResponseFields() = listOf(
        fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
        fieldWithPath("data.teamId").type(JsonFieldType.NUMBER).description("Team id"),
        fieldWithPath("data.name").type(JsonFieldType.STRING).description("Team name"),
        fieldWithPath("data.description").type(JsonFieldType.STRING).description("Team description").optional(),
        fieldWithPath("data.inviteCode").type(JsonFieldType.STRING).description("Invite code"),
        fieldWithPath("data.calendarEnabled").type(JsonFieldType.BOOLEAN).description("Calendar activation status"),
        fieldWithPath("data.matchEnabled").type(JsonFieldType.BOOLEAN).description("Match activation status"),
        fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
    )

    private fun memberResponseFields() = listOf(
        fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
        fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("Team member id"),
        fieldWithPath("data.userId").type(JsonFieldType.NUMBER).description("User id"),
        fieldWithPath("data.name").type(JsonFieldType.STRING).description("Member name"),
        fieldWithPath("data.email").type(JsonFieldType.STRING).description("Member email"),
        fieldWithPath("data.role").type(JsonFieldType.STRING).description("Updated team role"),
        fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
    )
}
