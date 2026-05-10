package swm.calender.core.api.controller.v1.team

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import jakarta.validation.Validation
import org.springframework.http.MediaType
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch
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
import swm.calender.core.api.controller.v1.team.request.TeamMemberRoleChangeRequest
import swm.calender.core.api.controller.v1.team.service.TeamAdministrationApiFacade
import swm.calender.core.api.controller.v1.team.service.TeamRequestValidator
import swm.calender.core.api.security.AuthenticatedUser
import swm.calender.core.common.id.TeamId
import swm.calender.core.common.id.TeamMemberId
import swm.calender.core.common.id.UserId
import swm.calender.core.enums.TeamMemberRole
import swm.calender.core.team.service.TeamAdministrationService
import swm.calender.core.team.service.request.TeamMemberRemovalRequest
import swm.calender.core.team.service.request.TeamMemberRoleChangeRequest as TeamServiceMemberRoleChangeRequest
import swm.calender.core.team.service.response.TeamMemberResponse as TeamServiceMemberResponse

class TeamAdministrationControllerTest : FunSpec() {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val restDocumentation = ManualRestDocumentation()
    private val validatorFactory = Validation.buildDefaultValidatorFactory()

    private lateinit var teamAdministrationService: TeamAdministrationService
    private lateinit var mockMvc: MockMvc

    init {
        beforeTest { testCase ->
            restDocumentation.beforeTest(javaClass, testCase.name.name)
            teamAdministrationService = mockk()

            val controller = TeamAdministrationController(
                teamAdministrationApiFacade = TeamAdministrationApiFacade(teamAdministrationService),
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

        test("owner can change a member role and document the administration endpoint") {
            // given
            every {
                teamAdministrationService.changeMemberRole(
                    TeamServiceMemberRoleChangeRequest(
                        teamId = TeamId(1L),
                        memberId = TeamMemberId(2L),
                        actorUserId = UserId(1L),
                        role = TeamMemberRole.OWNER,
                    ),
                )
            } returns teamMemberResponse(role = TeamMemberRole.OWNER)

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
        }

        test("owner can remove a member and document the administration endpoint") {
            // given
            every {
                teamAdministrationService.removeMember(
                    TeamMemberRemovalRequest(
                        teamId = TeamId(1L),
                        memberId = TeamMemberId(2L),
                        actorUserId = UserId(1L),
                    ),
                )
            } returns teamMemberResponse(role = TeamMemberRole.MEMBER)

            // when & then
            mockMvc.perform(
                delete("/api/v1/teams/{teamId}/members/{memberId}", 1L, 2L)
                    .principal(authenticatedUser(1L, "owner@swm.app", "Owner")),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.memberId").value(2L))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andDo(
                    document(
                        "teams-member-remove",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("teamId").description("Team id"),
                            parameterWithName("memberId").description("Team member id"),
                        ),
                        responseFields(
                            memberResponseFields(),
                        ),
                    ),
                )
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

    private fun teamMemberResponse(role: TeamMemberRole): TeamServiceMemberResponse {
        return TeamServiceMemberResponse(
            memberId = 2L,
            userId = 2L,
            name = "Member",
            email = "member@swm.app",
            role = role,
        )
    }

    private fun memberResponseFields() = listOf(
        fieldWithPath("result").type(JsonFieldType.STRING).description("API result"),
        fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("Team member id"),
        fieldWithPath("data.userId").type(JsonFieldType.NUMBER).description("User id"),
        fieldWithPath("data.name").type(JsonFieldType.STRING).description("Member name"),
        fieldWithPath("data.email").type(JsonFieldType.STRING).description("Member email"),
        fieldWithPath("data.role").type(JsonFieldType.STRING).description("Team role"),
        fieldWithPath("error").type(JsonFieldType.NULL).ignored(),
    )
}
